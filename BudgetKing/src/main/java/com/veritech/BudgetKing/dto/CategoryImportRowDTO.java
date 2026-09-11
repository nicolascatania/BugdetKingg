package com.veritech.BudgetKing.dto;

/**
 * A single data row of an imported category CSV file together with the outcome of validating it.
 *
 * <p>The CSV columns are, in order: {@code name,icon}.</p>
 *
 * @param lineNumber   1-based physical line of the CSV file this row was read from, used to
 *                     point the user at the offending line
 * @param name         category name as written in the file, mandatory
 * @param icon         icon that will actually be used: either the value from the file (when it
 *                     is a known icon) or {@link com.veritech.BudgetKing.service.CategoryIconCatalog#DEFAULT_ICON}
 *                     when the file's value is blank or unrecognised
 * @param valid        whether the row passed every validation rule
 * @param errorMessage human readable reason why the row is invalid, {@code null} when valid
 * @param duplicate    whether the user already has a category with this name
 */
public record CategoryImportRowDTO(
        int lineNumber,
        String name,
        String icon,
        boolean valid,
        String errorMessage,
        boolean duplicate
) {
}
