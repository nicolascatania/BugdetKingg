package com.veritech.BudgetKing.security.filter;

import com.veritech.BudgetKing.security.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code Authorization: Bearer} header and, when the token is valid
 * and belongs to an enabled user, populates the security context.
 * Any problem with the token (expired, tampered, unknown user, disabled account)
 * simply leaves the request unauthenticated so the entry point answers 401;
 * it never surfaces as a 500 and never leaks the reason to the caller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || path.startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(authHeader.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Sets the authentication for a valid token. Failures are logged at debug
     * level only, since malformed or expired tokens are routine noise.
     */
    private void authenticate(String token, HttpServletRequest request) {
        try {
            String email = jwtUtil.extractUsername(token);
            if (email == null) {
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!userDetails.isEnabled() || !jwtUtil.isTokenValid(token, userDetails)) {
                return;
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            log.debug("Rejected bearer token on {}: {}", request.getRequestURI(), ex.getMessage());
        }
    }
}
