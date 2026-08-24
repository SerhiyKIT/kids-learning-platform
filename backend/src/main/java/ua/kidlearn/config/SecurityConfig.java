package ua.kidlearn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import ua.kidlearn.ratelimit.AuthRateLimitFilter;
import ua.kidlearn.ratelimit.ClientIpResolver;
import ua.kidlearn.ratelimit.LoginAttemptService;
import ua.kidlearn.ratelimit.LoginThrottleFilter;
import ua.kidlearn.ratelimit.RateLimitProperties;
import ua.kidlearn.ratelimit.RateLimiter;
import ua.kidlearn.ratelimit.RateLimitingAuthenticationFailureHandler;
import ua.kidlearn.ratelimit.RateLimitingAuthenticationSuccessHandler;

/**
 * Session-based security for the web-first PWA.
 *
 * TODO: Google OAuth2 login, TOTP 2FA for admins, admin data-access audit log.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAttemptService loginAttemptService,
			ClientIpResolver clientIpResolver, RateLimiter rateLimiter, RateLimitProperties rateLimitProperties)
			throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/verify-email",
								"/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
						// Dev-only account creation (see ua.kidlearn.devauth) — same reasoning as
						// /api/auth/register: it must be reachable before a session exists. Under any
						// non-dev profile the controller itself doesn't exist, so this permitAll is a
						// no-op there (route unmapped -> 404 regardless of this rule).
						.requestMatchers(HttpMethod.POST, "/api/dev/register-role").permitAll()
						.requestMatchers("/login", "/error").permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf
						// Readable-by-JS cookie (SPA-friendly): the frontend reads XSRF-TOKEN and
						// echoes it back as the X-XSRF-TOKEN header on POST/PUT/DELETE requests.
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				// Forces the XSRF-TOKEN cookie to be written on the first response a
				// client makes, rather than only as a side effect of rendering a form.
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				// Per-IP counting on the other sensitive auth POSTs; placed ahead of CSRF so a
				// flood is rejected with 429 before paying for CSRF token validation.
				.addFilterBefore(new AuthRateLimitFilter(rateLimiter, clientIpResolver, rateLimitProperties),
						CsrfFilter.class)
				// Short-circuits over-the-limit login attempts (by account or IP) with 429 before
				// Spring Security's own auth filter runs, so a locked account can't keep probing.
				.addFilterBefore(new LoginThrottleFilter(loginAttemptService, clientIpResolver),
						UsernamePasswordAuthenticationFilter.class)
				.formLogin(form -> form.permitAll()
						.successHandler(new RateLimitingAuthenticationSuccessHandler(loginAttemptService))
						.failureHandler(new RateLimitingAuthenticationFailureHandler(loginAttemptService, clientIpResolver)))
				.logout(logout -> logout.permitAll())
				// This is a JSON API with no server-rendered pages to redirect to, so
				// unauthenticated access must return 401, not a 302 to /login.
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				// Same reasoning: an anonymous hit on a protected endpoint has no "page to return
				// to" once logged in. Without this, that hit still gets saved (via the default
				// HttpSessionRequestCache, independently of the 401 entry point above) and a
				// later successful login redirects to it instead of the frontend's default "/" —
				// e.g. a client's own CSRF-cookie warm-up GET could otherwise hijack the very
				// next login's redirect target.
				.requestCache(cache -> cache.disable());
		return http.build();
	}

}
