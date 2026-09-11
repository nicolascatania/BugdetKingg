package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.CategoryImportPreviewDTO;
import com.veritech.BudgetKing.dto.CategoryImportRowDTO;
import com.veritech.BudgetKing.exception.CategoryImportRuntimeException;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.repository.CategoryRepository;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the two-step CSV import flow for categories, mirroring
 * {@link TransactionImportService}.
 *
 * <p>1. {@link #preview(MultipartFile)} parses and validates the file without touching the
 * database, returning a {@link CategoryImportPreviewDTO} the client can render for review.</p>
 *
 * <p>2. {@link #commit(MultipartFile)} re-parses and re-validates the same file and persists
 * every row that is valid and not a duplicate, reusing {@link CategoryService#create} so it
 * behaves exactly like creating a category by hand.</p>
 *
 * <p>The CSV columns are, in order: {@code name,icon} (see {@link CategoryImportRowDTO} for the
 * exact per-column semantics). Unlike the account/category columns on the transaction import,
 * an unrecognised icon never invalidates the row: it silently falls back to
 * {@link CategoryIconCatalog#DEFAULT_ICON}.</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryImportService {

    private static final List<String> EXPECTED_HEADERS = List.of("name", "icon");

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final SecurityUtils securityUtils;

    /**
     * Parses and validates the uploaded CSV without persisting anything.
     */
    public CategoryImportPreviewDTO preview(MultipartFile file) {
        AppUser user = securityUtils.getCurrentUser();
        List<CategoryImportRowDTO> rows = parseRows(file, user);
        return buildSummary(rows);
    }

    /**
     * Re-parses and re-validates the uploaded CSV, then persists every row that is valid and
     * not a duplicate as a real category.
     *
     * @param file uploaded CSV file, expected to be the same one previously previewed
     * @return summary of the parsed rows, mirroring what would have been returned by
     * {@link #preview(MultipartFile)} for the same file
     */
    @Transactional
    public CategoryImportPreviewDTO commit(MultipartFile file) {
        AppUser user = securityUtils.getCurrentUser();

        List<CategoryImportRowDTO> rows = parseRows(file, user);

        for (CategoryImportRowDTO row : rows) {
            if (row.valid() && !row.duplicate()) {
                categoryService.create(new CategoryDTO(null, row.name(), row.icon()));
            }
        }

        return buildSummary(rows);
    }

    private List<CategoryImportRowDTO> parseRows(MultipartFile file, AppUser user) {
        if (file == null || file.isEmpty()) {
            throw new CategoryImportRuntimeException("CSV file is empty");
        }

        List<CategoryImportRowDTO> rows = new ArrayList<>();

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
            throw new CategoryImportRuntimeException("Could not read CSV file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new CategoryImportRuntimeException("CSV file has no data rows");
        }

        return rows;
    }

    private void validateHeader(List<String> headerNames) {
        List<String> normalized = headerNames.stream()
                .map(h -> h == null ? "" : h.trim().toLowerCase())
                .toList();

        if (!normalized.equals(EXPECTED_HEADERS)) {
            throw new CategoryImportRuntimeException(
                    "Invalid CSV header. Expected columns in this order: " + String.join(",", EXPECTED_HEADERS));
        }
    }

    /**
     * Wraps {@link #parseRow} so an unexpected error on a single malformed physical row (e.g. a
     * row with fewer columns than the header) turns into an invalid row instead of aborting the
     * whole import.
     */
    private CategoryImportRowDTO parseRowSafely(CSVRecord record, AppUser user) {
        try {
            return parseRow(record, user);
        } catch (Exception e) {
            int lineNumber = (int) record.getRecordNumber() + 1;
            return new CategoryImportRowDTO(
                    lineNumber,
                    safeGet(record, "name"),
                    CategoryIconCatalog.DEFAULT_ICON,
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

    private CategoryImportRowDTO parseRow(CSVRecord record, AppUser user) {
        int lineNumber = (int) record.getRecordNumber() + 1;

        String rawName = record.get("name");
        String rawIcon = record.get("icon");

        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlankOrNUll(rawName)) {
            errors.add("Name is mandatory");
        }

        // Golden rule: an unrecognised or blank icon never fails the row, it just falls back.
        String icon = CategoryIconCatalog.resolveOrDefault(rawIcon);

        boolean valid = errors.isEmpty();

        boolean duplicate = false;
        if (valid) {
            duplicate = categoryRepository.getByNameAndUser(rawName.trim(), user).isPresent();
        }

        return new CategoryImportRowDTO(
                lineNumber,
                rawName,
                icon,
                valid,
                valid ? null : String.join("; ", errors),
                duplicate
        );
    }

    private CategoryImportPreviewDTO buildSummary(List<CategoryImportRowDTO> rows) {
        int totalRows = rows.size();
        int errorRows = (int) rows.stream().filter(r -> !r.valid()).count();
        int duplicateRows = (int) rows.stream().filter(r -> r.valid() && r.duplicate()).count();
        int validRows = totalRows - errorRows - duplicateRows;

        return new CategoryImportPreviewDTO(rows, totalRows, validRows, duplicateRows, errorRows);
    }
}
