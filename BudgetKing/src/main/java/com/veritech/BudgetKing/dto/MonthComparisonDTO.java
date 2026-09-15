package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

/**
 * The current calendar month against the previous one, so the home page can say
 * whether the user is spending less, earning more or saving more than last month.
 *
 * @param currentIncome    income registered so far this month
 * @param currentExpense   expenses registered so far this month
 * @param previousIncome   income of the whole previous month
 * @param previousExpense  expenses of the whole previous month
 */
public record MonthComparisonDTO(
        BigDecimal currentIncome,
        BigDecimal currentExpense,
        BigDecimal previousIncome,
        BigDecimal previousExpense
) {

    /** Net result of the current month: {@code income - expense}. */
    public BigDecimal currentNet() {
        return currentIncome.subtract(currentExpense);
    }

    /** Net result of the previous month: {@code income - expense}. */
    public BigDecimal previousNet() {
        return previousIncome.subtract(previousExpense);
    }
}
