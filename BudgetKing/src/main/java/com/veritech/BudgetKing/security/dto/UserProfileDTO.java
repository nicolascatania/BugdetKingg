package com.veritech.BudgetKing.security.dto;

/** The authenticated user's own profile, as shown in the UI (e.g. the sidebar pill). */
public record UserProfileDTO(
        String email,
        String name,
        String lastName,
        String picture
) {
}
