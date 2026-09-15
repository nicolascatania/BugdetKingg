package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.ImportPreviewDTO;
import com.veritech.BudgetKing.dto.ImportRowDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.TransactionImportRuntimeException;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import com.veritech.BudgetKing.utils.DateUtils;
import com.veritech.BudgetKing.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the two-step CSV import flow for transactions.
 *
 * <p>1. {@link #preview(MultipartFile)} parses and validates the file without touching the
 * database, returning an {@link ImportPreviewDTO} the client can render for review.</p>
 *
 * <p>2. {@link #commit(MultipartFile)} re-parses and re-validates the same file and persists
 * every row that is valid and not a duplicate, reusing {@link TransactionService#create} so
 * balances are updated exactly the same way a manually created transaction would. Each row
 * carries its own target account (matched by name), so a single file can spread transactions
 * across every account the user has.</p>
 *
 * <p>The CSV columns are, in order:
 * {@code date,description,amount,type,category,counterparty,account,destination_account}
 * (see {@link ImportRowDTO} for the exact per-column semantics). {@code TRANSFER} rows require
 * {@code destination_account} (matched by name, must exist and differ from {@code account});
 * {@code category} is mandatory for INCOME/EXPENSE and optional for TRANSFER, mirroring
 * {@link TransactionDTO}'s own validation rules. A category name that does not exist for the
 * user is not an error: the preview flags it ({@code newCategory}) and the commit creates it
 * with {@link CategoryIconCatalog#DEFAULT_ICON} before persisting the row.</p>
 */
@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "date", "description", "amount", "type", "category", "counterparty",
            "account", "destination_account"
    );

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    /**
     * Parses and validates the uploaded CSV without persisting anything.
     *
     * @param file uploaded CSV file
     * @return summary of every row found, split into valid / duplicate / error buckets
     */
    public ImportPreviewDTO preview(MultipartFile file) {
        AppUser user = securityUtils.getCurrentUser();
        List<ImportRowDTO> rows = parseRows(file, user);
        return buildSummary(rows);
    }

    /**
     * Re-parses and re-validates the uploaded CSV, then persists every row that is valid and
     * not a duplicate as a real transaction in the account resolved for that row.
     *
     * @param file uploaded CSV file, expected to be the same one previously previewed
     * @return summary of the parsed rows, mirroring what would have been returned by
     * {@link #preview(MultipartFile)} for the same file
     */
    @Transactional
    public ImportPreviewDTO commit(MultipartFile file) {
        AppUser user = securityUtils.getCurrentUser();

        List<ImportRowDTO> rows = parseRows(file, user);

        // Categories created during this commit, so several rows naming the same new
        // category share one row instead of each inserting its own.
        Map<String, Category> createdCategories = new HashMap<>();

        for (ImportRowDTO row : rows) {
            if (row.valid() && !row.duplicate()) {
                persistRow(row, user, createdCategories);
            }
        }

        return buildSummary(rows);
    }

    private void persistRow(ImportRowDTO row, AppUser user, Map<String, Category> createdCategories) {
        Category category = null;
        if (!StringUtils.isBlankOrNUll(row.category())) {
            category = resolveOrCreateCategory(row.category(), user, createdCategories);
        }

        TransactionDTO dto = new TransactionDTO(
                null,
                row.date(),
                row.amount(),
                row.type(),
                row.counterparty(),
                row.description(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                row.account(),
                row.destinationAccount(),
                null,
                null,
                null
        );

        transactionService.create(dto);
    }

    /**
     * Reads the whole file and returns one {@link ImportRowDTO} per data row.
     *
     * <p>Whole-file problems (missing/empty file, unreadable content, header that does not
     * match the expected columns, no data rows at all) raise a
     * {@link TransactionImportRuntimeException}. Problems local to a single row never abort the
     * whole import: the offending row is simply flagged invalid so the rest of the file can
     * still be reviewed/imported.</p>
     */
    private List<ImportRowDTO> parseRows(MultipartFile file, AppUser user) {
        if (file == null || file.isEmpty()) {
            throw new TransactionImportRuntimeException("CSV file is empty");
        }

        List<ImportRowDTO> rows = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .get();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            validateHeader(parser.getHeaderNames());

            for (CSVRecord record : parser) {
                rows.add(parseRowSafely(record, user));
            }
        } catch (IOException | IllegalStateException e) {
            throw new TransactionImportRuntimeException("Could not read CSV file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new TransactionImportRuntimeException("CSV file has no data rows");
        }

        return rows;
    }

    private void validateHeader(List<String> headerNames) {
        List<String> normalized = headerNames.stream()
                .map(h -> h == null ? "" : h.trim().toLowerCase())
                .toList();

        if (!normalized.equals(EXPECTED_HEADERS)) {
            throw new TransactionImportRuntimeException(
                    "Invalid CSV header. Expected columns in this order: " + String.join(",", EXPECTED_HEADERS));
        }
    }

    /**
     * Wraps {@link #parseRow} so an unexpected error on a single malformed physical row (e.g. a
     * row with fewer columns than the header) turns into an invalid row instead of aborting the
     * whole import.
     */
    private ImportRowDTO parseRowSafely(CSVRecord record, AppUser user) {
        try {
            return parseRow(record, user);
        } catch (Exception e) {
            int lineNumber = (int) record.getRecordNumber() + 1;
            return new ImportRowDTO(
                    lineNumber,
                    safeGet(record, "date"),
                    safeGet(record, "description"),
                    null,
                    safeGet(record, "type"),
                    safeGet(record, "category"),
                    false,
                    safeGet(record, "counterparty"),
                    null,
                    null,
                    false,
                    "Malformed row: " + e.getMessage(),
                    false
            );
        }
    }

    private String safeGet(CSVRecord record, String column) {
        try {
            return record.get(column);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    private ImportRowDTO parseRow(CSVRecord record, AppUser user) {
        int lineNumber = (int) record.getRecordNumber() + 1;

        String rawDate = record.get("date");
        String rawDescription = record.get("description");
        String rawAmount = record.get("amount");
        String rawType = record.get("type");
        String rawCategory = record.get("category");
        String rawCounterparty = record.get("counterparty");
        String rawAccount = record.get("account");
        String rawDestinationAccount = record.get("destination_account");

        List<String> errors = new ArrayList<>();

        LocalDateTime parsedDate = null;
        try {
            parsedDate = parseDate(rawDate);
        } catch (DateTimeParseException | NullPointerException e) {
            errors.add("Invalid date format");
        }

        if (StringUtils.isBlankOrNUll(rawDescription)) {
            errors.add("Description is mandatory");
        }

        BigDecimal amount = null;
        try {
            amount = new BigDecimal(rawAmount.trim());
            if (amount.signum() <= 0) {
                errors.add("Amount must be greater than zero");
            }
        } catch (NumberFormatException | NullPointerException e) {
            errors.add("Invalid amount format");
        }

        TransactionType type = null;
        try {
            type = TransactionType.fromString(rawType);
        } catch (IllegalArgumentException e) {
            errors.add("Invalid transaction type: " + rawType);
        }
        // Savings movements are only produced through the savings-goal endpoints, where the
        // goal's own balance is kept consistent; a CSV row cannot carry that context.
        if (type != null && type.isSavings()) {
            errors.add("Savings transactions cannot be imported: " + rawType);
        }
        boolean isTransfer = type == TransactionType.TRANSFER;

        // Category is mandatory for INCOME/EXPENSE, optional for TRANSFER - mirrors
        // TransactionDTO.isCategoryRequired().
        // A name that does not exist yet is not an error: the commit creates it (default
        // icon) and the preview flags it so the user knows before confirming.
        String categoryName = StringUtils.isBlankOrNUll(rawCategory) ? null : rawCategory.trim();
        boolean newCategory = false;
        if (!isTransfer && categoryName == null) {
            errors.add("Category is mandatory");
        }
        if (categoryName != null) {
            newCategory = categoryRepository.getByNameAndUser(categoryName, user).isEmpty();
        }

        String counterparty = StringUtils.isBlankOrNUll(rawCounterparty) ? "Unknown" : rawCounterparty.trim();

        UUID account = null;
        Account resolvedAccount = null;
        if (StringUtils.isBlankOrNUll(rawAccount)) {
            errors.add("Account is mandatory");
        } else {
            resolvedAccount = accountRepository.findByNameAndUser(rawAccount.trim(), user).orElse(null);
            if (resolvedAccount == null) {
                errors.add("Account not found: " + rawAccount);
            } else {
                account = resolvedAccount.getId();
            }
        }

        // destination_account is only relevant for TRANSFER rows - mirrors
        // TransactionDTO.isDestinationAccountValid() and TransactionService.validateTransaction().
        UUID destinationAccount = null;
        if (isTransfer) {
            if (StringUtils.isBlankOrNUll(rawDestinationAccount)) {
                errors.add("Destination account is mandatory for TRANSFER transactions");
            } else {
                Account resolvedDestination =
                        accountRepository.findByNameAndUser(rawDestinationAccount.trim(), user).orElse(null);
                if (resolvedDestination == null) {
                    errors.add("Destination account not found: " + rawDestinationAccount);
                } else if (resolvedAccount != null && resolvedDestination.getId().equals(resolvedAccount.getId())) {
                    errors.add("Source and destination accounts must be different");
                } else {
                    destinationAccount = resolvedDestination.getId();
                }
            }
        }

        boolean valid = errors.isEmpty();

        boolean duplicate = false;
        if (valid) {
            duplicate = transactionRepository.existsByUserAndDateAndAmountAndDescriptionAndType(
                    user, parsedDate, amount, rawDescription.trim(), type);
        }

        String dateOut = parsedDate != null ? parsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : rawDate;
        String typeOut = type != null ? type.name() : rawType;

        return new ImportRowDTO(
                lineNumber,
                dateOut,
                rawDescription,
                amount,
                typeOut,
                categoryName,
                newCategory,
                counterparty,
                account,
                destinationAccount,
                valid,
                valid ? null : String.join("; ", errors),
                duplicate
        );
    }

    /**
     * Accepts {@code dd/MM/yyyy HH:mm}, {@code dd/MM/yyyy} and both ISO forms; see
     * {@link DateUtils#parseCsvDate(String)}.
     */
    private LocalDateTime parseDate(String raw) {
        if (StringUtils.isBlankOrNUll(raw)) {
            throw new DateTimeParseException("Date is mandatory", raw == null ? "" : raw, 0);
        }
        return DateUtils.parseCsvDate(raw.trim());
    }

    private ImportPreviewDTO buildSummary(List<ImportRowDTO> rows) {
        int totalRows = rows.size();
        int errorRows = (int) rows.stream().filter(r -> !r.valid()).count();
        int duplicateRows = (int) rows.stream().filter(r -> r.valid() && r.duplicate()).count();
        int validRows = totalRows - errorRows - duplicateRows;

        List<String> newCategories = rows.stream()
                .filter(r -> r.valid() && !r.duplicate() && r.newCategory())
                .map(ImportRowDTO::category)
                .distinct()
                .toList();

        return new ImportPreviewDTO(rows, totalRows, validRows, duplicateRows, errorRows, newCategories);
    }

    /**
     * Finds the user's category by exact name, or creates it with the default icon. Newly
     * created ones are remembered in {@code createdCategories} for the rest of the commit.
     */
    private Category resolveOrCreateCategory(String name, AppUser user, Map<String, Category> createdCategories) {
        Category cached = createdCategories.get(name);
        if (cached != null) {
            return cached;
        }
        return categoryRepository.getByNameAndUser(name, user).orElseGet(() -> {
            Category created = categoryRepository.save(Category.builder()
                    .name(name)
                    .icon(CategoryIconCatalog.DEFAULT_ICON)
                    .user(user)
                    .build());
            createdCategories.put(name, created);
            return created;
        });
    }
}
