package com.veritech.BudgetKing.security.controller;

import com.veritech.BudgetKing.dto.GoogleTokenInfoDTO;
import com.veritech.BudgetKing.exception.LocalAuthDisabledException;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Role;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.repository.RoleRepository;
import com.veritech.BudgetKing.security.dto.AuthResponse;
import com.veritech.BudgetKing.security.dto.GoogleAuthRequest;
import com.veritech.BudgetKing.security.dto.LoginRequest;
import com.veritech.BudgetKing.security.enumerator.AuthProvider;
import com.veritech.BudgetKing.security.enumerator.Roles;
import com.veritech.BudgetKing.security.service.GoogleAuthService;
import com.veritech.BudgetKing.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Specification")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private GoogleAuthService googleAuthService;

    @InjectMocks
    private AuthController authController;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().name(Roles.ROLE_USER.name()).build();
    }

    @Test
    @DisplayName("Should reject /login while local auth is disabled (the current default)")
    void loginDisabledByDefault() {
        ReflectionTestUtils.setField(authController, "localAuthEnabled", false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("password1");

        assertThrows(LocalAuthDisabledException.class, () -> authController.login(request));
        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("Should create a new user and return a token on first Google sign-in")
    void createsUserOnFirstGoogleSignIn() {
        GoogleTokenInfoDTO tokenInfo = new GoogleTokenInfoDTO(
                "client-id", "google-sub-1", "new@gmail.com", "true", "Ada", "Lovelace", "https://example.com/ada.jpg"
        );
        when(googleAuthService.verify("id-token")).thenReturn(tokenInfo);
        when(appUserRepository.findByProviderId("google-sub-1")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(Roles.ROLE_USER.name())).thenReturn(Optional.of(userRole));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.google(new GoogleAuthRequest("id-token"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new AuthResponse("jwt-token"), response.getBody());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("new@gmail.com", saved.getEmail());
        assertEquals("google-sub-1", saved.getProviderId());
        assertEquals(AuthProvider.GOOGLE, saved.getAuthProvider());
        assertEquals("Ada", saved.getName());
        assertEquals("Lovelace", saved.getLastName());
        assertNull(saved.getPasswordHash());
    }

    @Test
    @DisplayName("Should reuse the existing user when the Google subject already matches")
    void reusesExistingGoogleUser() {
        GoogleTokenInfoDTO tokenInfo = new GoogleTokenInfoDTO(
                "client-id", "google-sub-1", "returning@gmail.com", "true", "Ada", "Lovelace", null
        );
        AppUser existing = AppUser.builder()
                .email("returning@gmail.com")
                .providerId("google-sub-1")
                .authProvider(AuthProvider.GOOGLE)
                .name("Ada")
                .lastName("Lovelace")
                .build();
        when(googleAuthService.verify("id-token")).thenReturn(tokenInfo);
        when(appUserRepository.findByProviderId("google-sub-1")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        authController.google(new GoogleAuthRequest("id-token"));

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should link a Google sign-in to a pre-existing local account with the same email")
    void linksGoogleToExistingLocalAccountByEmail() {
        GoogleTokenInfoDTO tokenInfo = new GoogleTokenInfoDTO(
                "client-id", "google-sub-2", "legacy@gmail.com", "true", "Ada", "Lovelace", null
        );
        AppUser legacyLocalUser = AppUser.builder()
                .email("legacy@gmail.com")
                .authProvider(AuthProvider.LOCAL)
                .passwordHash("bcrypt-hash")
                .name("Ada")
                .lastName("Lovelace")
                .build();
        when(googleAuthService.verify("id-token")).thenReturn(tokenInfo);
        when(appUserRepository.findByProviderId("google-sub-2")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("legacy@gmail.com")).thenReturn(Optional.of(legacyLocalUser));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        authController.google(new GoogleAuthRequest("id-token"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertEquals("google-sub-2", captor.getValue().getProviderId());
        assertEquals(AuthProvider.GOOGLE, captor.getValue().getAuthProvider());
    }
}
