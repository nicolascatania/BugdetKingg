package com.veritech.BudgetKing.dto;

import java.util.List;
import java.util.UUID;

public record AppUserForListDTO(
        UUID id,
        String name,
        String lastName,
        String email,
        Boolean enabled,
        List<String> roles

) {
}
