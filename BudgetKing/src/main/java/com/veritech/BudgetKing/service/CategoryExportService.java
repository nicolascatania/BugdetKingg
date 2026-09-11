package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.repository.CategoryRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports the current user's categories to CSV.
 *
 * <p>The output uses the exact same column layout expected by
 * {@link CategoryImportService} ({@code name,icon}), so a file exported here can be
 * re-imported unchanged.</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryExportService {

    private static final String[] HEADERS = {"name", "icon"};

    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    /**
     * Builds a CSV file with every category of the current user.
     *
     * @return the CSV file content, UTF-8 encoded
     */
    public byte[] exportToCsv() {
        AppUser user = securityUtils.getCurrentUser();
        List<Category> categories = categoryRepository.findByUser(user);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .get();

        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            for (Category c : categories) {
                printer.printRecord(c.getName(), c.getIcon());
            }
            printer.flush();
        } catch (IOException e) {
            // Writing to an in-memory ByteArrayOutputStream never actually fails; the checked
            // exception only exists because CSVPrinter implements java.io.Closeable/Flushable.
            throw new UncheckedIOException("Could not generate CSV export", e);
        }

        return out.toByteArray();
    }
}
