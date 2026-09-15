package com.veritech.BudgetKing.security.controller;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.security.UserDetailsImpl;
import com.veritech.BudgetKing.security.config.SecurityConfig;
import com.veritech.BudgetKing.security.dto.UpdateProfileRequest;
import com.veritech.BudgetKing.security.dto.UserProfileDTO;
import com.veritech.BudgetKing.security.service.UserProfileService;
import com.veritech.BudgetKing.security.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP contract of {@code /me}: auth, validation, CORS for the PATCH the settings page sends. */
@WebMvcTest(UserProfileController.class)
@Import(SecurityConfig.class)
@DisplayName("User profile endpoints")
class UserProfileControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static UserDetailsImpl principal(String country) {
        AppUser user = AppUser.builder()
                .id(USER_ID)
                .email("ada@mail.com")
                .name("Ada")
                .lastName("Lovelace")
                .country(country)
                .build();
        return new UserDetailsImpl(user);
    }

    @Test
    @DisplayName("GET /me includes the stored country")
    void meIncludesCountry() throws Exception {
        mockMvc.perform(get("/me").with(user(principal("AR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@mail.com"))
                .andExpect(jsonPath("$.country").value("AR"));
    }

    @Test
    @DisplayName("GET /me returns null country while unset")
    void meNullCountryWhileUnset() throws Exception {
        mockMvc.perform(get("/me").with(user(principal(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value((Object) null));
    }

    @Test
    @DisplayName("PATCH /me forwards the caller's id and returns the stored profile")
    void patchUpdatesOwnProfile() throws Exception {
        when(userProfileService.update(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(new UserProfileDTO("ada@mail.com", "Ada", "Lovelace", null, "PL"));

        mockMvc.perform(patch("/me")
                        .with(user(principal(null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"PL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("PL"));

        verify(userProfileService).update(eq(USER_ID), eq(new UpdateProfileRequest("PL")));
    }

    @Test
    @DisplayName("PATCH /me rejects a malformed country before reaching the service")
    void patchRejectsMalformedCountry() throws Exception {
        mockMvc.perform(patch("/me")
                        .with(user(principal(null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"argentina\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(userProfileService);
    }

    @Test
    @DisplayName("PATCH /me maps an unknown code to 400")
    void patchMapsUnknownCodeTo400() throws Exception {
        when(userProfileService.update(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenThrow(new IllegalArgumentException("Unknown country code: XX"));

        mockMvc.perform(patch("/me")
                        .with(user(principal(null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"XX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("PATCH /me requires authentication")
    void patchRequiresAuth() throws Exception {
        mockMvc.perform(patch("/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"country\":\"AR\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userProfileService);
    }

    @Test
    @DisplayName("CORS preflight allows PATCH from the frontend origin")
    void corsPreflightAllowsPatch() throws Exception {
        mockMvc.perform(options("/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }
}
