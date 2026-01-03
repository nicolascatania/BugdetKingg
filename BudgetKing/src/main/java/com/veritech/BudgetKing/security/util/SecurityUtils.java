package com.veritech.BudgetKing.security.util;

import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new IllegalStateException("No authenticated user");
        }

        return userDetails.getUser();
    }
}
