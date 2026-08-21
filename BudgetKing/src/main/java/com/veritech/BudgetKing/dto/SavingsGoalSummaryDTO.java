package com.veritech.BudgetKing.dto;

import java.math.BigDecimal;

/**
 * Aggregated snapshot of every savings goal owned by the current user.
 *
 * @param totalSaved         sum of the balances backing all goals
 * @param totalTarget        sum of every goal target
 * @param progressPercentage {@code totalSaved / totalTarget * 100}, zero when there is nothing to reach
 * @param totalGoals         how many goals the user has
 * @param achievedGoals      how many of them are already covered
 */
public record SavingsGoalSummaryDTO(
        BigDecimal totalSaved,
        BigDecimal totalTarget,
        BigDecimal progressPercentage,
        long totalGoals,
        long achievedGoals
) {
}
