package com.veritech.BudgetKing.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client token bucket in front of the public {@code /auth/**} endpoints
 * (login and register) to slow down credential brute force and mass sign-ups.
 * <p>
 * Each client IP gets {@code capacity} requests, refilled in full every
 * {@code refillMinutes}. Once empty the request is rejected with 429 and a
 * {@code Retry-After} header. Buckets live in memory, which is enough for a
 * single instance; a shared store (Redis) would be needed to scale out.
 */
@Component
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    /** Soft cap on tracked clients; idle (full) buckets are dropped past it. */
    static final int MAX_TRACKED_CLIENTS = 10_000;

    private static final String AUTH_PATH_PREFIX = "/auth";

    private final long capacity;
    private final Duration refillPeriod;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.auth.rate-limit.capacity}") long capacity,
            @Value("${app.auth.rate-limit.refill-minutes}") long refillMinutes
    ) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofMinutes(refillMinutes);
    }

    /** Only the auth endpoints are throttled; preflight requests carry no credentials. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith(AUTH_PATH_PREFIX) || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientKey = request.getRemoteAddr();
        ConsumptionProbe probe = bucketFor(clientKey).tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.warn("Rate limit hit on {} from {}", request.getRequestURI(), clientKey);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many attempts, try again later.\"}");
    }

    private Bucket bucketFor(String clientKey) {
        evictIdleBucketsIfNeeded();
        return buckets.computeIfAbsent(clientKey, key -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, refillPeriod)
                        .build())
                .build());
    }

    /**
     * Keeps the map bounded without a scheduler: past the cap, drop every bucket
     * that has fully refilled, i.e. clients that have not called in a while.
     */
    private void evictIdleBucketsIfNeeded() {
        if (buckets.size() < MAX_TRACKED_CLIENTS) {
            return;
        }
        buckets.entrySet().removeIf(entry -> entry.getValue().getAvailableTokens() >= capacity);
    }
}
