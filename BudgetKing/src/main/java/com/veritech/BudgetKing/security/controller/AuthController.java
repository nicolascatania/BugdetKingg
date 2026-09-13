package com.veritech.BudgetKing.security.controller;

import com.veritech.BudgetKing.dto.GoogleTokenInfoDTO;
import com.veritech.BudgetKing.exception.LocalAuthDisabledException;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Role;
import com.veritech.BudgetKing.repository.AppUserRepository;
import com.veritech.BudgetKing.repository.RoleRepository;
import com.veritech.BudgetKing.security.UserDetailsImpl;
import com.veritech.BudgetKing.security.dto.AuthResponse;
import com.veritech.BudgetKing.security.dto.GoogleAuthRequest;
import com.veritech.BudgetKing.security.dto.LoginRequest;
import com.veritech.BudgetKing.security.dto.RegisterRequest;
import com.veritech.BudgetKing.security.enumerator.AuthProvider;
import com.veritech.BudgetKing.security.enumerator.Roles;
import com.veritech.BudgetKing.security.service.GoogleAuthService;
import com.veritech.BudgetKing.security.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final GoogleAuthService googleAuthService;

    /**
     * Email/password sign-in is currently switched off in favour of
     * Google-only auth; {@code /login} and {@code /register} below stay
     * implemented, gated by this flag, so the fallback can be turned back on
     * without rebuilding it. See {@code docs/proposals/} for the reasoning.
     */
    @Value("${app.auth.local-enabled:false}")
    private boolean localAuthEnabled;

    /**
     * Email/password login. Disabled by default — see {@link #localAuthEnabled}.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        requireLocalAuthEnabled();

        try {
             Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            var userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(token));

        }catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }



    }

    /**
     * Email/password sign-up. Disabled by default — see {@link #localAuthEnabled}.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        requireLocalAuthEnabled();

        if (appUserRepository.existsByEmail(request.email())) {
            return ResponseEntity
                    .badRequest()
                    .body("Email already in use");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setName(request.name());
        user.setLastName(request.lastName());

        Role userRole = roleRepository.findByName(Roles.ROLE_USER.name())
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        user.setRoles(Set.of(userRole));

        appUserRepository.save(user);

        var userDetails = new UserDetailsImpl(user);

        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    /**
     * Google Sign-In: verifies the ID token the frontend obtained from Google
     * Identity Services, finds or creates the matching {@link AppUser}, and
     * issues this app's own JWT — the frontend never sees or stores Google's token.
     */
    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody @Valid GoogleAuthRequest request) {
        GoogleTokenInfoDTO tokenInfo = googleAuthService.verify(request.idToken());

        AppUser user = appUserRepository.findByProviderId(tokenInfo.subject())
                .or(() -> appUserRepository.findByEmail(tokenInfo.email()))
                .map(existing -> linkGoogleAccount(existing, tokenInfo))
                .orElseGet(() -> createGoogleUser(tokenInfo));

        var userDetails = new UserDetailsImpl(user);
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    private void requireLocalAuthEnabled() {
        if (!localAuthEnabled) {
            throw new LocalAuthDisabledException("Email/password sign-in is disabled. Use Google sign-in.");
        }
    }

    /**
     * Existing user signing in with Google — first time (legacy local account)
     * or a returning Google user. The photo is refreshed on every login since
     * it can change on Google's side; identity fields are only set once.
     */
    private AppUser linkGoogleAccount(AppUser user, GoogleTokenInfoDTO tokenInfo) {
        boolean firstLink = user.getProviderId() == null;
        boolean pictureChanged = !Objects.equals(user.getPicture(), tokenInfo.picture());

        if (firstLink) {
            user.setProviderId(tokenInfo.subject());
            user.setAuthProvider(AuthProvider.GOOGLE);
        }
        if (pictureChanged) {
            user.setPicture(tokenInfo.picture());
        }
        if (firstLink || pictureChanged) {
            appUserRepository.save(user);
        }
        return user;
    }

    private AppUser createGoogleUser(GoogleTokenInfoDTO tokenInfo) {
        AppUser user = new AppUser();
        user.setEmail(tokenInfo.email());
        user.setProviderId(tokenInfo.subject());
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setPasswordHash(null);
        user.setEnabled(true);
        // Google may omit given/family name on some accounts; never leave the required column null.
        user.setName(blankToFallback(tokenInfo.givenName(), "Google"));
        user.setLastName(blankToFallback(tokenInfo.familyName(), "User"));
        user.setPicture(tokenInfo.picture());

        Role userRole = roleRepository.findByName(Roles.ROLE_USER.name())
                .orElseThrow(() -> new RuntimeException("Role USER not found"));
        user.setRoles(Set.of(userRole));

        return appUserRepository.save(user);
    }

    private String blankToFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
