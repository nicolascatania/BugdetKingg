package com.veritech.BudgetKing.security.controller;

import com.veritech.BudgetKing.security.UserDetailsImpl;
import com.veritech.BudgetKing.security.dto.UpdateProfileRequest;
import com.veritech.BudgetKing.security.dto.UserProfileDTO;
import com.veritech.BudgetKing.security.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the signed-in user's own profile (name, last name, photo, country)
 * for the frontend to render — e.g. the sidebar user pill and the Settings
 * page. Deliberately outside {@code /auth/**}: that path is public and skips
 * the JWT filter entirely (see
 * {@link com.veritech.BudgetKing.security.filter.JwtAuthenticationFilter}),
 * while these endpoints must be authenticated.
 */
@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileDTO me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return UserProfileDTO.from(principal.getUser());
    }

    /** Partial update of the caller's own profile; returns the profile as stored. */
    @PatchMapping("/me")
    public UserProfileDTO update(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userProfileService.update(principal.getUser().getId(), request);
    }
}
