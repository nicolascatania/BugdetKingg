package com.veritech.BudgetKing.security.dto;

import jakarta.validation.constraints.NotBlank;

/** ID token the Google Identity Services client obtains in the browser. */
public record GoogleAuthRequest(
        @NotBlank
        String idToken
) {}
