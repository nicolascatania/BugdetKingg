package com.veritech.BudgetKing.dto;

import com.veritech.BudgetKing.model.AppUser;
import jakarta.validation.constraints.NotNull;

public record CategoryRelatedEntities(
        @NotNull(message = "User must be provided")
        AppUser user
) {
}