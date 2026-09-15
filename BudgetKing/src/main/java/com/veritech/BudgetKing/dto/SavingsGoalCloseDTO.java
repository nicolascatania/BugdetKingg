package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.enumerator.SavingsGoalCloseOutcome;

import java.util.UUID;

/**
 * Request body for {@code POST /savings-goal/{id}/close}.
 *
 * @param accountId  account that receives everything the goal still holds (and, for
 *                   {@code SPEND}, the one the expense is recorded against). May be
 *                   {@code null} only when the goal is already empty.
 * @param outcome    whether the money is returned or was spent on the goal's purpose;
 *                   {@code null} behaves as {@link SavingsGoalCloseOutcome#RETURN}
 * @param categoryId optional category for the expense recorded by {@code SPEND};
 *                   ignored for {@code RETURN}
 */
public record SavingsGoalCloseDTO(UUID accountId, SavingsGoalCloseOutcome outcome, UUID categoryId) {

    /** Convenience for callers that only care about the destination account. */
    public SavingsGoalCloseDTO(UUID accountId) {
        this(accountId, null, null);
    }

    /** Resolves the outcome, defaulting to returning the money. */
    public SavingsGoalCloseOutcome resolvedOutcome() {
        return outcome != null ? outcome : SavingsGoalCloseOutcome.RETURN;
    }
}
