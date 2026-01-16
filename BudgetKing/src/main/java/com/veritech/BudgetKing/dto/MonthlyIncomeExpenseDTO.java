package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

/**
 * Represents the income and expense totals for a given month.
 */
public record MonthlyIncomeExpenseDTO(
        int month,               // Month number (1-12)
        BigDecimal income,        // Total income for the month
        BigDecimal expense        // Total expense for the month
) {}