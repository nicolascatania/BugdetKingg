package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Figures for the dashboard over a date range.
 *
 * @param totalBalance       sum of every account balance right now; not tied to the range
 * @param expense            expenses registered inside the range
 * @param income             income registered inside the range
 * @param netBalance         {@code income - expense} for the range; negative when more went out than came in
 * @param expensesByCategory the range's expenses grouped by category, with their share of the total
 */
public record DashBoardDTO(
        BigDecimal totalBalance,
        BigDecimal expense,
        BigDecimal income,
        BigDecimal netBalance,
        List<CategoryExpenseDTO> expensesByCategory
) {
}
