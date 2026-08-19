package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import jakarta.validation.constraints.NotNull;

/**
 * JPA entities already resolved by the service and handed to the mapper.
 *
 * <p>Keeping resolution out of the mapper is what lets it stay a plain
 * {@code @Component} with no repository dependency.</p>
 */
public record RecurringTransactionRelatedEntities(
        @NotNull(message = "User must be provided")
        AppUser user,
        @NotNull(message = "Account must be provided")
        Account account,
        Account destinationAccount,
        Category category) {}
