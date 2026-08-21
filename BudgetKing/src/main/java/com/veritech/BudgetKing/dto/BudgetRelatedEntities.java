package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import jakarta.validation.constraints.NotNull;

/**
 * Already-resolved JPA entities a {@link BudgetDTO} needs to become a
 * {@link com.veritech.BudgetKing.model.Budget}, so the mapper stays free of repositories.
 */
public record BudgetRelatedEntities(
        @NotNull(message = "User must be provided")
        AppUser user,
        @NotNull(message = "A budget must have a category")
        Category category
) {
}
