package com.veritech.BudgetKing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Claims Google's {@code tokeninfo} endpoint returns for a valid ID token.
 * See <a href="https://developers.google.com/identity/sign-in/web/backend-auth">
 * Google's backend auth guide</a>. Google serializes {@code email_verified} as
 * the string {@code "true"}/{@code "false"}, not a JSON boolean, hence
 * {@link #isEmailVerified()} instead of a boolean field.
 */
public record GoogleTokenInfoDTO(
        @JsonProperty("aud") String audience,
        @JsonProperty("sub") String subject,
        @JsonProperty("email") String email,
        @JsonProperty("email_verified") String emailVerified,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName
) {
    public boolean isEmailVerified() {
        return "true".equalsIgnoreCase(emailVerified);
    }
}
