package com.veritech.BudgetKing.security.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auth rate limit filter")
class AuthRateLimitFilterTest {

    private static final int CAPACITY = 3;

    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthRateLimitFilter(CAPACITY, 1);
    }

    @Test
    @DisplayName("lets a client through until its bucket is empty, then answers 429")
    void blocksAfterCapacity() throws ServletException, IOException {
        for (int i = 0; i < CAPACITY; i++) {
            MockHttpServletResponse response = call("/auth/login", "10.0.0.1");
            assertEquals(HttpStatus.OK.value(), response.getStatus(), "request " + (i + 1) + " should pass");
        }

        MockHttpServletResponse blocked = call("/auth/login", "10.0.0.1");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    @DisplayName("keeps a separate bucket per client IP")
    void isolatesClients() throws ServletException, IOException {
        for (int i = 0; i < CAPACITY; i++) {
            call("/auth/login", "10.0.0.1");
        }

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), call("/auth/login", "10.0.0.1").getStatus());
        assertEquals(HttpStatus.OK.value(), call("/auth/register", "10.0.0.2").getStatus());
    }

    @Test
    @DisplayName("login and register share the same bucket for one client")
    void sharesBucketAcrossAuthEndpoints() throws ServletException, IOException {
        call("/auth/login", "10.0.0.1");
        call("/auth/register", "10.0.0.1");
        call("/auth/login", "10.0.0.1");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), call("/auth/register", "10.0.0.1").getStatus());
    }

    @Test
    @DisplayName("ignores every path outside /auth")
    void skipsNonAuthPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account/by-user");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    @DisplayName("ignores CORS preflight requests on /auth")
    void skipsPreflight() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    @DisplayName("throttles POST requests on /auth")
    void filtersAuthPosts() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        assertFalse(filter.shouldNotFilter(request));
    }

    private MockHttpServletResponse call(String path, String clientIp) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
