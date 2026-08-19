package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single data row of an imported CSV file together with the outcome of validating it.
 *
 * <p>The CSV columns are, in order: {@code date,description,amount,type,category,counterparty}.
 * The {@code account} field is not part of the file: the client stamps the target account on
 * every row before confirming the import, because a transaction cannot exist without one.</p>
 *
 * @param lineNumber   1-based physical line of the CSV file this row was read from, used to
 *                     point the user at the offending line
 * @param date         when the row is valid, the date normalised to ISO-8601 local date-time
 *                     ({@code yyyy-MM-ddTHH:mm:ss}) so it can be fed straight into
 *                     {@code TransactionMapper.toEntity}; otherwise the raw text as typed
 * @param description  free text describing the movement, mandatory
 * @param amount       parsed amount, {@code null} when the cell could not be read as a number
 * @param type         transaction type as written in the file; only INCOME and EXPENSE can be
 *                     imported, TRANSFER needs a destination account the file cannot carry
 * @param category     category name as written in the file; it must already exist for the user
 * @param counterparty who was paid / who paid, defaulted when the cell is blank
 * @param account      target account the movement will be created in, filled by the client
 * @param valid        whether the row passed every validation rule
 * @param errorMessage human readable reason why the row is invalid, {@code null} when valid
 * @param duplicate    whether an existing transaction already matches this row
 */
public record ImportRowDTO(
        int lineNumber,
        String date,
        String description,
        BigDecimal amount,
        String type,
        String category,
        String counterparty,
        UUID account,
        boolean valid,
        String errorMessage,
        boolean duplicate
) {
}
