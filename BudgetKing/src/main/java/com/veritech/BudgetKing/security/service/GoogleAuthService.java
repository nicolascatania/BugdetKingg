package com.veritech.BudgetKing.security.service;

import com.veritech.BudgetKing.client.GoogleTokenInfoClient;
import com.veritech.BudgetKing.dto.GoogleTokenInfoDTO;
import com.veritech.BudgetKing.exception.InvalidGoogleTokenException;
import com.veritech.BudgetKing.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Verifies a Google Sign-In ID token and returns its claims. Verification is
 * delegated to Google's {@code tokeninfo} endpoint (see
 * {@link GoogleTokenInfoClient}), which validates the token's signature,
 * issuer and expiry on Google's side.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleTokenInfoClient googleTokenInfoClient;

    /** The OAuth client id this backend was registered with in Google Cloud Console. */
    @Value("${app.auth.google.client-id:}")
    private String googleClientId;

    /**
     * Verifies {@code idToken} and returns its claims.
     *
     * @throws InvalidGoogleTokenException if Google rejects the token, it was
     *         issued for a different OAuth client, or its email is unverified
     */
    public GoogleTokenInfoDTO verify(String idToken) {
        if (StringUtils.isBlankOrNUll(googleClientId)) {
            // Fails closed: a misconfigured deployment must not silently accept any client's token.
            throw new InvalidGoogleTokenException("Google sign-in is not configured on this server");
        }

        GoogleTokenInfoDTO tokenInfo;
        try {
            tokenInfo = googleTokenInfoClient.getTokenInfo(idToken);
        } catch (RestClientException e) {
            throw new InvalidGoogleTokenException("Google rejected the token (expired, malformed or revoked)");
        }

        if (!googleClientId.equals(tokenInfo.audience())) {
            throw new InvalidGoogleTokenException("Token was not issued for this application");
        }
        if (!tokenInfo.isEmailVerified()) {
            throw new InvalidGoogleTokenException("Google has not verified this email");
        }

        return tokenInfo;
    }
}
