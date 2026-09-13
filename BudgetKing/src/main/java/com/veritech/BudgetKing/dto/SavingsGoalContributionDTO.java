package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request body for moving money between an account and a savings goal
 * ({@code POST /savings-goal/{id}/deposit} and {@code /withdraw}).
 *
 * @param accountId account the money leaves (deposit) or returns to (withdrawal)
 * @param amount    positive amount to move
 * @param date      when the movement happened; defaults to now
 * @param note      optional description stored on the generated transaction;
 *                  defaults to {@code "Savings · <goal name>"}
 */
public record SavingsGoalContributionDTO(
        @NotNull(message = "Account is mandatory")
        UUID accountId,

        @NotNull(message = "Amount is mandatory")
        BigDecimal amount,

        LocalDateTime date,
        String note
) {
}
