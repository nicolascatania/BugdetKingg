package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only view of how much of a budget has already been spent during its period.
 *
 * @param budgetId        identifier of the budget being reported
 * @param categoryId      identifier of the budgeted category
 * @param categoryName    display name of the budgeted category
 * @param categoryIcon    Font Awesome icon of the budgeted category
 * @param limitAmount     the limit the user set for the period
 * @param spentAmount     expenses already registered for the category in that period
 * @param remainingAmount limit minus spent; negative once the budget is exceeded
 * @param percentage      spent over limit, expressed from 0 to 100 (and beyond)
 * @param status          {@code OK}, {@code WARNING} (>= 80%) or {@code EXCEEDED} (>= 100%)
 */
public record BudgetProgressDTO(
        UUID budgetId,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        double percentage,
        String status
) {
}
