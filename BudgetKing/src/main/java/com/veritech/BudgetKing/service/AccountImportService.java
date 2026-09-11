package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.AccountDTO;
import com.veritech.BudgetKing.dto.AccountImportPreviewDTO;
import com.veritech.BudgetKing.dto.AccountImportRowDTO;
import com.veritech.BudgetKing.exception.AccountImportRuntimeException;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.AccountRepository;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the two-step CSV import flow for accounts, mirroring {@link CategoryImportService}.
 *
 * <p>1. {@link #preview(MultipartFile)} parses and validates the file without touching the
 * database, returning an {@link AccountImportPreviewDTO} the client can render for review.</p>
 *
 * <p>2. {@link #commit(MultipartFile)} re-parses and re-validates the same file and persists
 * every row that is valid and not a duplicate, reusing {@link AccountService#create} so it
 * behaves exactly like creating an account by hand. Every imported account starts with a
 * balance of zero; the file cannot carry an opening balance.</p>
 *
 * <p>The CSV columns are, in order: {@code name,description,icon} (see
 * {@link AccountImportRowDTO} for the exact per-column semantics). An unrecognised icon never
 * invalidates the row: it silently falls back to {@link AccountIconCatalog#DEFAULT_ICON}.</p>
 */
@Service
@RequiredArgsConstructor
public class AccountImportService {

    private static final List<String> EXPECTED_HEADERS = List.of("name", "description", "icon");

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final SecurityUtils securityUtils;

    /**
     * Parses and validates the uploaded CSV without persisting anything.
     */
    public AccountImportPreviewDTO preview(MultipartFile file) {
        AppUser user = securityUtils.getCurrentUser();
        List<AccountImportRowDTO> rows = parseRows(file, user);
        return buildSummary(rows);
    }

    /**
     * Re-parses and re-validates the uploaded CSV, then persists every row that is valid and
     * not a duplicate as a real account.
     *
     * @param file uploaded CSV file, expected to be the same one previously previewed
     * @return summary of the parsed rows, mirroring what would have been returned by
     * {@link #preview(MultipartFile)} for the same file
     */
    @Transactional
    public AccountImportPreviewDTO commit(MultipartFile file) {
        AppUser user = securityUtils.getCurrentUser();

        List<AccountImportRowDTO> rows = parseRows(file, user);

        for (AccountImportRowDTO row : rows) {
            if (row.valid() && !row.duplicate()) {
                accountService.create(new AccountDTO(null, row.name(), row.description(), row.icon(), BigDecimal.ZERO));
            }
        }

        return buildSummary(rows);
    }

    private List<AccountImportRowDTO> parseRows(MultipartFile file, AppUser user) {
        if (file == null || file.isEmpty()) {
            throw new AccountImportRuntimeException("CSV file is empty");
        }

        List<AccountImportRowDTO> rows = new ArrayList<>();

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
            throw new AccountImportRuntimeException("Could not read CSV file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new AccountImportRuntimeException("CSV file has no data rows");
        }

        return rows;
    }

    private void validateHeader(List<String> headerNames) {
        List<String> normalized = headerNames.stream()
                .map(h -> h == null ? "" : h.trim().toLowerCase())
                .toList();

        if (!normalized.equals(EXPECTED_HEADERS)) {
            throw new AccountImportRuntimeException(
                    "Invalid CSV header. Expected columns in this order: " + String.join(",", EXPECTED_HEADERS));
        }
    }

    /**
     * Wraps {@link #parseRow} so an unexpected error on a single malformed physical row (e.g. a
     * row with fewer columns than the header) turns into an invalid row instead of aborting the
     * whole import.
     */
    private AccountImportRowDTO parseRowSafely(CSVRecord record, AppUser user) {
        try {
            return parseRow(record, user);
        } catch (Exception e) {
            int lineNumber = (int) record.getRecordNumber() + 1;
            return new AccountImportRowDTO(
                    lineNumber,
                    safeGet(record, "name"),
                    safeGet(record, "description"),
                    AccountIconCatalog.DEFAULT_ICON,
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

    private AccountImportRowDTO parseRow(CSVRecord record, AppUser user) {
        int lineNumber = (int) record.getRecordNumber() + 1;

        String rawName = record.get("name");
        String rawDescription = record.get("description");
        String rawIcon = record.get("icon");

        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlankOrNUll(rawName)) {
            errors.add("Name is mandatory");
        }

        if (StringUtils.isBlankOrNUll(rawDescription)) {
            errors.add("Description is mandatory");
        }

        // Golden rule: an unrecognised or blank icon never fails the row, it just falls back.
        String icon = AccountIconCatalog.resolveOrDefault(rawIcon);

        boolean valid = errors.isEmpty();

        boolean duplicate = false;
        if (valid) {
            duplicate = accountRepository.findByNameAndUser(rawName.trim(), user).isPresent();
        }

        return new AccountImportRowDTO(
                lineNumber,
                rawName,
                rawDescription,
                icon,
                valid,
                valid ? null : String.join("; ", errors),
                duplicate
        );
    }

    private AccountImportPreviewDTO buildSummary(List<AccountImportRowDTO> rows) {
        int totalRows = rows.size();
        int errorRows = (int) rows.stream().filter(r -> !r.valid()).count();
        int duplicateRows = (int) rows.stream().filter(r -> r.valid() && r.duplicate()).count();
        int validRows = totalRows - errorRows - duplicateRows;

        return new AccountImportPreviewDTO(rows, totalRows, validRows, duplicateRows, errorRows);
    }
}
