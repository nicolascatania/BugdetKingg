package com.veritech.BudgetKing.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sign-up payload. The password policy is enforced here, server side, regardless
 * of what the client validates: 8 to 72 characters (BCrypt ignores anything past
 * 72 bytes) containing at least one letter and one digit.
 */
public record RegisterRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
        String password,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 100)
        String lastName
) {}
