package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDTO(
        UUID id,
        @NotBlank(message = "Name is mandatory")
        String name,
        @NotBlank(message = "Description is mandatory")
        String description,
        @NotBlank(message = "Icon is mandatory")
        String icon,
        BigDecimal balance
) {
}
