package com.veritech.BudgetKing.security.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Body of {@code PATCH /me}. {@code country} is an ISO 3166-1 alpha-2 code, or
 * {@code null} to clear the choice. Absent and {@code null} mean the same
 * thing on purpose: country is the only profile field the user can edit, so
 * there is nothing yet that would need to tell them apart.
 */
public record UpdateProfileRequest(
        @Pattern(regexp = "[A-Z]{2}", message = "Country must be an ISO 3166-1 alpha-2 code")
        String country
) {
}
