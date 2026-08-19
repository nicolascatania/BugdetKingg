package com.veritech.BudgetKing.dto;

import java.util.List;

/**
 * Result of a dry-run CSV import. Nothing is persisted while building it: the user reviews
 * this summary and only then confirms which rows should become transactions.
 *
 * @param rows           every data row of the file, in the order they appear
 * @param totalRows      number of data rows found (the header is not counted)
 * @param validRows      rows that can be imported right away: valid and not duplicated
 * @param duplicateRows  rows that are valid but already exist for the user
 * @param errorRows      rows that failed at least one validation rule
 */
public record ImportPreviewDTO(
        List<ImportRowDTO> rows,
        int totalRows,
        int validRows,
        int duplicateRows,
        int errorRows
) {
}
