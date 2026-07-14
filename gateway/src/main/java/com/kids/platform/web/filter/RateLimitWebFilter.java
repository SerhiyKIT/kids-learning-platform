package com.kids.platform.web.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * In-memory rate limiter using token-bucket counters backed by Caffeine.
 * No Redis dependency required — suitable for single-node dev/staging.
 *
 * Limits:
 *  - AI hint endpoint: 10 requests / 60s per user (or IP if unauthenticated)
 *  - All other API endpoints: 200 requests / 60s per IP
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitWebFilter implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitWebFilter.class);

    private static final int AI_LIMIT = 10;
    private static final int GENERAL_LIMIT = 200;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private static final String AI_PATH = "/services/aicontentservice/api/generate-hint";
    private static final String RATE_LIMIT_EXCEEDED =
        "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Перевищено ліміт запитів. Спробуйте через хвилину.\"}";

    // Cache: key → request count in current window (expires after WINDOW)
    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
        .expireAfterWrite(WINDOW)
        .maximumSize(50_000)
        .build();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Only rate-limit API paths
        if (!path.startsWith("/api") && !path.startsWith("/services")) {
            return chain.filter(exchange);
        }

        String remoteIp = getClientIp(exchange);
        boolean isAiPath = path.startsWith(AI_PATH);

        if (isAiPath) {
            // Stricter limit on AI endpoint — key by user identity
            return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication() != null ? ctx.getAuthentication().getName() : remoteIp)
                .defaultIfEmpty(remoteIp)
                .flatMap(identity -> enforceLimit("ai:" + identity, AI_LIMIT, exchange, chain));
        }

        return enforceLimit("ip:" + remoteIp, GENERAL_LIMIT, exchange, chain);
    }

    private Mono<Void> enforceLimit(String key, int limit, ServerWebExchange exchange, WebFilterChain chain) {
        AtomicInteger counter = counters.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - current)));

        if (current > limit) {
            LOG.warn("Rate limit exceeded for key={} count={}", key, current);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            var buffer = exchange.getResponse().bufferFactory().wrap(RATE_LIMIT_EXCEEDED.getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var address = exchange.getRequest().getRemoteAddress();
        return address != null ? address.getAddress().getHostAddress() : "unknown";
    }
}
