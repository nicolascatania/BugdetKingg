package com.veritech.BudgetKing.dto;


import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.SavingsGoal;
import jakarta.validation.constraints.NotNull;

/**
 * Already-resolved JPA entities a {@link TransactionDTO} needs to become a
 * {@code Transaction}. The mapper never touches repositories, so the service
 * resolves these first.
 *
 * @param savingsGoal goal fed or drained by a savings movement, {@code null} otherwise
 */
public record TransactionRelatedEntities(
        @NotNull(message = "User must be provided")
        AppUser user,
        @NotNull(message = "Account must be provided")
        Account account,
        Account destinationAccount,
        Category Category,
        SavingsGoal savingsGoal) {

    /** Convenience for the common case: a movement that is not linked to a goal. */
    public TransactionRelatedEntities(AppUser user, Account account, Account destinationAccount, Category category) {
        this(user, account, destinationAccount, category, null);
    }
}
