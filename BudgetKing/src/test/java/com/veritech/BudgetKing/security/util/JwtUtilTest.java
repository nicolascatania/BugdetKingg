package com.veritech.BudgetKing.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT util")
class JwtUtilTest {

    /** Same test-only key as src/test/resources/application.properties. */
    private static final String SECRET = "c3VwZXItc2VjcmV0LWtleS1idWRnZXQta2luZy0yNTZiaXRz";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWZvci1idWRnZXQta2luZy0yNTY=";

    private JwtUtil jwtUtil;
    private UserDetails nico;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        nico = User.withUsername("nico@budgetking.com").password("x").roles("USER").build();
    }

    @Test
    @DisplayName("accepts a freshly issued token for its own user")
    void validForOwner() {
        String token = jwtUtil.generateToken(nico);

        assertTrue(jwtUtil.isTokenValid(token, nico));
        assertEquals("nico@budgetking.com", jwtUtil.extractUsername(token));
    }

    @Test
    @DisplayName("rejects a token whose subject is a different user")
    void rejectsSubjectMismatch() {
        String token = jwtUtil.generateToken(nico);
        UserDetails someoneElse = User.withUsername("other@budgetking.com").password("x").roles("USER").build();

        assertFalse(jwtUtil.isTokenValid(token, someoneElse));
    }

    @Test
    @DisplayName("rejects an expired token")
    void rejectsExpired() {
        String expired = Jwts.builder()
                .setSubject(nico.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(expired, nico));
    }

    @Test
    @DisplayName("rejects a token signed with another key")
    void rejectsForeignSignature() {
        String forged = Jwts.builder()
                .setSubject(nico.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET)), SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(forged, nico));
    }

    @Test
    @DisplayName("rejects garbage")
    void rejectsGarbage() {
        assertFalse(jwtUtil.isTokenValid("not.a.jwt", nico));
    }
}
