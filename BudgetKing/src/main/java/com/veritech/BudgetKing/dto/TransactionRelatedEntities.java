package com.veritech.BudgetKing.dto;


import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import jakarta.validation.constraints.NotNull;

public record TransactionRelatedEntities(
        @NotNull(message = "User must be provided")
        AppUser user,
        @NotNull(message = "Account must be provided")
        Account account,
        Account destinationAccount,
        @NotNull(message = "A transaction must have a category")
        Category Category) {}