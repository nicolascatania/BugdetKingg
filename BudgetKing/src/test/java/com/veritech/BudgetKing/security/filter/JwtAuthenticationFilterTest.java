package com.veritech.BudgetKing.security.filter;

import com.veritech.BudgetKing.security.util.JwtUtil;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT authentication filter")
class JwtAuthenticationFilterTest {

    private static final String EMAIL = "nico@budgetking.com";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("authenticates the request when the token is valid and the user is enabled")
    void authenticatesValidToken() throws ServletException, IOException {
        UserDetails user = user(true);
        when(jwtUtil.extractUsername("good")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(user);
        when(jwtUtil.isTokenValid("good", user)).thenReturn(true);

        MockFilterChain chain = run("Bearer good");

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(EMAIL, SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotNull(chain.getRequest(), "the chain must continue");
    }

    @Test
    @DisplayName("swallows malformed or expired tokens and continues unauthenticated")
    void tolerantOfBrokenTokens() throws ServletException, IOException {
        when(jwtUtil.extractUsername("broken")).thenThrow(new MalformedJwtException("bad token"));

        MockFilterChain chain = assertDoesNotThrow(() -> run("Bearer broken"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest(), "the chain must continue so the entry point can answer 401");
    }

    @Test
    @DisplayName("does not authenticate a disabled account even with a valid token")
    void rejectsDisabledUser() throws ServletException, IOException {
        when(jwtUtil.extractUsername("good")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(user(false));

        run("Bearer good");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).isTokenValid(anyString(), any());
    }

    @Test
    @DisplayName("continues unauthenticated when the token subject no longer exists")
    void tolerantOfUnknownUser() throws ServletException, IOException {
        when(jwtUtil.extractUsername("orphan")).thenReturn("gone@budgetking.com");
        when(userDetailsService.loadUserByUsername("gone@budgetking.com"))
                .thenThrow(new UsernameNotFoundException("not found"));

        assertDoesNotThrow(() -> run("Bearer orphan"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("skips requests without a bearer header")
    void skipsMissingHeader() throws ServletException, IOException {
        run(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    private MockFilterChain run(String authorizationHeader) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/by-user");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private static UserDetails user(boolean enabled) {
        return User.withUsername(EMAIL)
                .password("irrelevant")
                .roles("USER")
                .disabled(!enabled)
                .build();
    }
}
