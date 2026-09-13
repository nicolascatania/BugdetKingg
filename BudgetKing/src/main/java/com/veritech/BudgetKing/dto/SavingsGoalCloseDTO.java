package com.veritech.BudgetKing.dto;

import java.util.UUID;

/**
 * Request body for {@code POST /savings-goal/{id}/close}.
 *
 * @param accountId account that receives everything the goal still holds. May be
 *                  {@code null} only when the goal is already empty.
 */
public record SavingsGoalCloseDTO(UUID accountId) {
}
