package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import jakarta.validation.constraints.NotNull;

/**
 * Already-resolved JPA entities a {@link SavingsGoalDTO} needs to become a
 * {@code SavingsGoal}. The mapper never touches repositories, so the service
 * resolves these first.
 *
 * @param user          owner of the goal, always required
 * @param linkedAccount optional account funding the goal
 */
public record SavingsGoalRelatedEntities(
        @NotNull(message = "User must be provided")
        AppUser user,
        Account linkedAccount
) {
}
