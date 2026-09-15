package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single data row of an imported CSV file together with the outcome of validating it.
 *
 * <p>The CSV columns are, in order:
 * {@code date,description,amount,type,category,counterparty,account,destination_account}.</p>
 *
 * @param lineNumber        1-based physical line of the CSV file this row was read from, used
 *                          to point the user at the offending line
 * @param date              when the row is valid, the date normalised to ISO-8601 local
 *                          date-time ({@code yyyy-MM-ddTHH:mm:ss}) so it can be fed straight
 *                          into {@code TransactionMapper.toEntity}; otherwise the raw text as
 *                          typed
 * @param description       free text describing the movement, mandatory
 * @param amount            parsed amount, {@code null} when the cell could not be read as a
 *                          number
 * @param type              transaction type as written in the file (INCOME, EXPENSE or
 *                          TRANSFER)
 * @param category          category name as written in the file (trimmed); mandatory except
 *                          for TRANSFER rows where it's optional. It does not need to exist:
 *                          see {@code newCategory}
 * @param newCategory       {@code true} when the category does not exist for the user yet and
 *                          committing the import will create it with the default icon
 * @param counterparty      who was paid / who paid, defaulted when the cell is blank
 * @param account           source account resolved by matching the file's {@code account}
 *                          column against the user's accounts by name
 * @param destinationAccount destination account for TRANSFER rows, resolved the same way as
 *                          {@code account}; always {@code null} for INCOME/EXPENSE rows
 * @param valid             whether the row passed every validation rule
 * @param errorMessage      human readable reason why the row is invalid, {@code null} when
 *                          valid
 * @param duplicate         whether an existing transaction already matches this row
 */
public record ImportRowDTO(
        int lineNumber,
        String date,
        String description,
        BigDecimal amount,
        String type,
        String category,
        boolean newCategory,
        String counterparty,
        UUID account,
        UUID destinationAccount,
        boolean valid,
        String errorMessage,
        boolean duplicate
) {
}
