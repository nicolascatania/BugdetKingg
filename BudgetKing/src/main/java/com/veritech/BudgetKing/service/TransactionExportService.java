package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports the current user's transactions to CSV.
 *
 * <p>The output uses the exact same column layout expected by
 * {@link TransactionImportService}
 * ({@code date,description,amount,type,category,counterparty,account,destination_account}),
 * so a file exported here can be re-imported unchanged.</p>
 */
@Service
@RequiredArgsConstructor
public class TransactionExportService {

    private static final String[] HEADERS = {
            "date", "description", "amount", "type", "category", "counterparty",
            "account", "destination_account"
    };

    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    /**
     * Builds a CSV file with every transaction of the current user matching the given filter.
     *
     * @param filter optional filter (dates, amount range, account, category, type, text);
     *               when {@code null} every transaction of the user is exported
     * @return the CSV file content, UTF-8 encoded
     */
    public byte[] exportToCsv(TransactionFilter filter) {
        AppUser user = securityUtils.getCurrentUser();

        List<Transaction> transactions = (filter != null)
                ? transactionRepository.findAll(filter.toSpecification(user), Sort.by(Sort.Direction.DESC, "date"))
                : transactionRepository.findByUser(user);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .get();

        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            for (Transaction t : transactions) {
                printer.printRecord(
                        t.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        t.getDescription(),
                        t.getAmount(),
                        t.getType().name(),
                        t.getCategory() != null ? t.getCategory().getName() : "",
                        t.getCounterparty() != null ? t.getCounterparty() : "",
                        t.getAccount() != null ? t.getAccount().getName() : "",
                        t.getDestinationAccount() != null ? t.getDestinationAccount().getName() : ""
                );
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
