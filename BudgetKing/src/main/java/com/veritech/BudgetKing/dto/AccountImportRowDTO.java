package com.veritech.BudgetKing.dto;

/**
 * A single data row of an imported account CSV file together with the outcome of validating it.
 *
 * <p>The CSV columns are, in order: {@code name,description,icon}. Imported accounts always
 * start with a balance of zero; the file cannot carry an opening balance.</p>
 *
 * @param lineNumber   1-based physical line of the CSV file this row was read from, used to
 *                     point the user at the offending line
 * @param name         account name as written in the file, mandatory
 * @param description  free text describing the account, mandatory
 * @param icon         icon that will actually be used: either the value from the file (when it
 *                     is a known icon) or {@link com.veritech.BudgetKing.service.AccountIconCatalog#DEFAULT_ICON}
 *                     when the file's value is blank or unrecognised
 * @param valid        whether the row passed every validation rule
 * @param errorMessage human readable reason why the row is invalid, {@code null} when valid
 * @param duplicate    whether the user already has an account with this name
 */
public record AccountImportRowDTO(
        int lineNumber,
        String name,
        String description,
        String icon,
        boolean valid,
        String errorMessage,
        boolean duplicate
) {
}
