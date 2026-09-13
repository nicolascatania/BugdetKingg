package com.veritech.BudgetKing.security.service;

import com.veritech.BudgetKing.client.GoogleTokenInfoClient;
import com.veritech.BudgetKing.dto.GoogleTokenInfoDTO;
import com.veritech.BudgetKing.exception.InvalidGoogleTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Google Auth Service Specification")
class GoogleAuthServiceTest {

    private static final String CLIENT_ID = "expected-client-id";

    @Mock
    private GoogleTokenInfoClient googleTokenInfoClient;

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        googleAuthService = new GoogleAuthService(googleTokenInfoClient);
        ReflectionTestUtils.setField(googleAuthService, "googleClientId", CLIENT_ID);
    }

    @Test
    @DisplayName("Should return the token's claims when audience matches and email is verified")
    void shouldVerifyValidToken() {
        GoogleTokenInfoDTO tokenInfo = new GoogleTokenInfoDTO(
                CLIENT_ID, "sub-123", "user@gmail.com", "true", "Ada", "Lovelace"
        );
        when(googleTokenInfoClient.getTokenInfo("valid-token")).thenReturn(tokenInfo);

        GoogleTokenInfoDTO result = googleAuthService.verify("valid-token");

        assertEquals("user@gmail.com", result.email());
        assertEquals("sub-123", result.subject());
    }

    @Test
    @DisplayName("Should reject a token issued for a different OAuth client")
    void shouldRejectWrongAudience() {
        GoogleTokenInfoDTO tokenInfo = new GoogleTokenInfoDTO(
                "someone-elses-client-id", "sub-123", "user@gmail.com", "true", "Ada", "Lovelace"
        );
        when(googleTokenInfoClient.getTokenInfo("valid-token")).thenReturn(tokenInfo);

        assertThrows(InvalidGoogleTokenException.class, () -> googleAuthService.verify("valid-token"));
    }

    @Test
    @DisplayName("Should reject a token whose email Google has not verified")
    void shouldRejectUnverifiedEmail() {
        GoogleTokenInfoDTO tokenInfo = new GoogleTokenInfoDTO(
                CLIENT_ID, "sub-123", "user@gmail.com", "false", "Ada", "Lovelace"
        );
        when(googleTokenInfoClient.getTokenInfo("valid-token")).thenReturn(tokenInfo);

        assertThrows(InvalidGoogleTokenException.class, () -> googleAuthService.verify("valid-token"));
    }

    @Test
    @DisplayName("Should reject a token Google itself refuses (expired, malformed, revoked)")
    void shouldRejectTokenGoogleRefuses() {
        when(googleTokenInfoClient.getTokenInfo("bad-token")).thenThrow(new RestClientException("400"));

        assertThrows(InvalidGoogleTokenException.class, () -> googleAuthService.verify("bad-token"));
    }

    @Test
    @DisplayName("Should fail closed when no Google client id is configured")
    void shouldFailClosedWithoutClientId() {
        ReflectionTestUtils.setField(googleAuthService, "googleClientId", "");

        assertThrows(InvalidGoogleTokenException.class, () -> googleAuthService.verify("any-token"));
    }
}
