package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * How much the user has spent on a single category: one month in detail plus the
 * all-time figure. Only {@code EXPENSE} transactions count.
 *
 * @param categoryId       identifier of the category
 * @param categoryName     display name of the category
 * @param categoryIcon     Font Awesome icon of the category
 * @param year             calendar year of the detailed month
 * @param month            calendar month of the detailed month, 1 through 12
 * @param monthTotal       expenses registered in that month
 * @param monthCount       number of expenses in that month
 * @param allTimeTotal     expenses registered since the first transaction
 * @param allTimeCount     number of expenses ever registered
 * @param monthTransactions the month's expenses, most recent first
 */
public record CategorySpendingDTO(
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        int year,
        int month,
        BigDecimal monthTotal,
        long monthCount,
        BigDecimal allTimeTotal,
        long allTimeCount,
        List<LastMovesDTO> monthTransactions
) {
}
