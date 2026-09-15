package com.veritech.BudgetKing.security.dto;

import com.veritech.BudgetKing.model.AppUser;

/**
 * The authenticated user's own profile, as shown in the UI (e.g. the sidebar
 * pill and the Settings page). {@code country} is ISO 3166-1 alpha-2, or null
 * while the user has not chosen one.
 */
public record UserProfileDTO(
        String email,
        String name,
        String lastName,
        String picture,
        String country
) {

    public static UserProfileDTO from(AppUser user) {
        return new UserProfileDTO(
                user.getEmail(),
                user.getName(),
                user.getLastName(),
                user.getPicture(),
                user.getCountry()
        );
    }
}
