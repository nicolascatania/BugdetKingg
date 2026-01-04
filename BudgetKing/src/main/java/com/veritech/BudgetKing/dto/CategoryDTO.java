package com.veritech.BudgetKing.dto;

import java.util.UUID;

public record CategoryDTO(
        UUID id,
        String name,
        String description
) {
}
