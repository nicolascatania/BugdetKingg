package com.veritech.BudgetKing.security.controller;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.security.UserDetailsImpl;
import com.veritech.BudgetKing.security.dto.UserProfileDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the signed-in user's own profile (name, last name, photo) for the
 * frontend to render — e.g. the sidebar user pill. Deliberately outside
 * {@code /auth/**}: that path is public and skips the JWT filter entirely
 * (see {@link com.veritech.BudgetKing.security.filter.JwtAuthenticationFilter}),
 * while this endpoint must be authenticated.
 */
@RestController
public class UserProfileController {

    @GetMapping("/me")
    public UserProfileDTO me(@AuthenticationPrincipal UserDetailsImpl principal) {
        AppUser user = principal.getUser();
        return new UserProfileDTO(user.getEmail(), user.getName(), user.getLastName(), user.getPicture());
    }
}
