package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryDTO(
        UUID id,
        @NotBlank(message = "Name is mandatory")
        String name,

        @NotBlank(message = "Description is mandatory")
        String description
) {
}
