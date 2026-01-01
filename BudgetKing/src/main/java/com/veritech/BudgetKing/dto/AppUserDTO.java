package com.veritech.BudgetKing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record AppUserDTO(

        UUID id,

        @Email
        @NotBlank
        String email,

        @NotBlank
        String password, // plaintext just for create/update

        Set<String> roles // just names, for lecture

) {
}
