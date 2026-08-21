package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.ImportPreviewDTO;
import com.veritech.BudgetKing.dto.ImportRowDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.TransactionImportRuntimeException;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the two-step CSV import flow for transactions.
 *
 * <p>1. {@link #preview(MultipartFile)} parses and validates the file without touching the
 * database, returning an {@link ImportPreviewDTO} the client can render for review.</p>
 *
 * <p>2. {@link #commit(MultipartFile, UUID)} re-parses and re-validates the same file (the
 * client re-sends it together with the account chosen for the import) and persists every row
 * that is valid and not a duplicate, reusing {@link TransactionService#create} so balances are
 * updated exactly the same way a manually created transaction would.</p>
 *
 * <p>The CSV columns are, in order: {@code date,description,amount,type,category,counterparty}
 * (see {@link ImportRowDTO} for the exact per-column semantics). Only {@code INCOME} and
 * {@code EXPENSE} rows can be imported; {@code TRANSFER} needs a destination account the file
 * cannot carry.</p>
 */
@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private static final List<String> EXPECTED_HEADERS =
            List.of("date", "description", "amount", "type", "category", "counterparty");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountService accountService;
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
        List<ImportRowDTO> rows = parseRows(file, user, null);
        return buildSummary(rows);
    }

    /**
     * Re-parses and re-validates the uploaded CSV, then persists every row that is valid and
     * not a duplicate as a real transaction in the given account.
     *
     * @param file      uploaded CSV file, expected to be the same one previously previewed
     * @param accountId account the imported transactions will be created in; must belong to
     *                  the current user
     * @return summary of the parsed rows, mirroring what would have been returned by
     * {@link #preview(MultipartFile)} for the same file
     */
    @Transactional
    public ImportPreviewDTO commit(MultipartFile file, UUID accountId) {
        AppUser user = securityUtils.getCurrentUser();

        // Validates the account exists and belongs to the current user before doing any work.
        accountService.getEntityById(accountId);

        List<ImportRowDTO> rows = parseRows(file, user, accountId);

        for (ImportRowDTO row : rows) {
            if (row.valid() && !row.duplicate()) {
                persistRow(row, user, accountId);
            }
        }

        return buildSummary(rows);
    }

    private void persistRow(ImportRowDTO row, AppUser user, UUID accountId) {
        Category category = categoryRepository.getByNameAndUser(row.category(), user)
                .orElseThrow(() -> new TransactionImportRuntimeException(
                        "Category not found: " + row.category()));

        TransactionDTO dto = new TransactionDTO(
                null,
                row.date(),
                row.amount(),
                row.type(),
                row.counterparty(),
                row.description(),
                category.getId(),
                category.getName(),
                accountId,
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
    private List<ImportRowDTO> parseRows(MultipartFile file, AppUser user, UUID account) {
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
                rows.add(parseRowSafely(record, user, account));
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
    private ImportRowDTO parseRowSafely(CSVRecord record, AppUser user, UUID account) {
        try {
            return parseRow(record, user, account);
        } catch (Exception e) {
            int lineNumber = (int) record.getRecordNumber() + 1;
            return new ImportRowDTO(
                    lineNumber,
                    safeGet(record, "date"),
                    safeGet(record, "description"),
                    null,
                    safeGet(record, "type"),
                    safeGet(record, "category"),
                    safeGet(record, "counterparty"),
                    account,
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

    private ImportRowDTO parseRow(CSVRecord record, AppUser user, UUID account) {
        int lineNumber = (int) record.getRecordNumber() + 1;

        String rawDate = record.get("date");
        String rawDescription = record.get("description");
        String rawAmount = record.get("amount");
        String rawType = record.get("type");
        String rawCategory = record.get("category");
        String rawCounterparty = record.get("counterparty");

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
            if (type == TransactionType.TRANSFER) {
                errors.add("TRANSFER transactions cannot be imported");
                type = null;
            }
        } catch (IllegalArgumentException e) {
            errors.add("Invalid transaction type: " + rawType);
        }

        Category category = null;
        if (StringUtils.isBlankOrNUll(rawCategory)) {
            errors.add("Category is mandatory");
        } else {
            category = categoryRepository.getByNameAndUser(rawCategory.trim(), user).orElse(null);
            if (category == null) {
                errors.add("Category not found: " + rawCategory);
            }
        }

        String counterparty = StringUtils.isBlankOrNUll(rawCounterparty) ? "Unknown" : rawCounterparty.trim();
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
                rawCategory,
                counterparty,
                account,
                valid,
                valid ? null : String.join("; ", errors),
                duplicate
        );
    }

    /**
     * Accepts either a full ISO-8601 local date-time ({@code yyyy-MM-ddTHH:mm:ss}) or a bare
     * ISO local date ({@code yyyy-MM-dd}), which is normalised to midnight.
     */
    private LocalDateTime parseDate(String raw) {
        if (StringUtils.isBlankOrNUll(raw)) {
            throw new DateTimeParseException("Date is mandatory", raw == null ? "" : raw, 0);
        }
        String trimmed = raw.trim();
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
    }

    private ImportPreviewDTO buildSummary(List<ImportRowDTO> rows) {
        int totalRows = rows.size();
        int errorRows = (int) rows.stream().filter(r -> !r.valid()).count();
        int duplicateRows = (int) rows.stream().filter(r -> r.valid() && r.duplicate()).count();
        int validRows = totalRows - errorRows - duplicateRows;

        return new ImportPreviewDTO(rows, totalRows, validRows, duplicateRows, errorRows);
    }
}
