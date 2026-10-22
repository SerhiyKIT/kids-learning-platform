package ua.kidlearn.config;

import java.time.Clock;
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
import ua.kidlearn.ratelimit.TwoFactorRateLimitFilter;
import ua.kidlearn.twofa.TwoFactorGateFilter;
import ua.kidlearn.twofa.TwoFactorSessionAuthenticationSuccessHandler;
import ua.kidlearn.users.ActiveAccountFilter;
import ua.kidlearn.users.UserRepository;

/**
 * Session-based security for the web-first PWA.
 *
 * TODO: Google OAuth2 login, admin data-access audit log.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAttemptService loginAttemptService,
			ClientIpResolver clientIpResolver, RateLimiter rateLimiter, RateLimitProperties rateLimitProperties,
			UserRepository userRepository, AppProperties appProperties, Clock clock) throws Exception {
		// JS-readable by design (the SPA echoes it back as X-XSRF-TOKEN), but Secure in prod and
		// SameSite=Lax like the session cookie.
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setCookieCustomizer(cookie -> cookie
				.secure(appProperties.security().secureCookies())
				.sameSite("Lax"));
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/verify-email",
								"/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/accept-invite").permitAll()
						// Dev-only account creation (see ua.kidlearn.devauth) — same reasoning as
						// /api/auth/register: it must be reachable before a session exists. Under any
						// non-dev profile the controller itself doesn't exist, so this permitAll is a
						// no-op there (route unmapped -> 404 regardless of this rule).
						.requestMatchers(HttpMethod.POST, "/api/dev/register-role").permitAll()
						// Not @Profile-restricted (must work in prod, unlike /api/dev/register-role) — it's
						// self-securing instead: BootstrapService 404s unless a token is configured, 410s once
						// any ADMIN exists, and 401s on a wrong token (constant-time compare). See
						// ua.kidlearn.bootstrap's package javadoc. Also must be reachable pre-session.
						// The resulting first admin lands in the mandatory-2FA "setup required" gate on
						// their first login, same as any other admin — see ua.kidlearn.twofa.
						.requestMatchers(HttpMethod.POST, "/api/bootstrap/admin").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers("/login", "/error").permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf
						// Readable-by-JS cookie (SPA-friendly): the frontend reads XSRF-TOKEN and
						// echoes it back as the X-XSRF-TOKEN header on POST/PUT/DELETE requests.
						.csrfTokenRepository(csrfTokenRepository)
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
						.successHandler(new TwoFactorSessionAuthenticationSuccessHandler(loginAttemptService, userRepository, clock))
						.failureHandler(new RateLimitingAuthenticationFailureHandler(loginAttemptService, clientIpResolver)))
				.logout(logout -> logout.permitAll())
				// Both need the already-authenticated session's Authentication, so they only make
				// sense after formLogin's own filter has run. Rate-limit first (fail fast on abuse
				// before evaluating gate state), then the gate itself.
				.addFilterAfter(new TwoFactorRateLimitFilter(rateLimiter, clientIpResolver, rateLimitProperties),
						UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(new TwoFactorGateFilter(), TwoFactorRateLimitFilter.class)
				// A session opened before its account was deactivated stops working on its next
				// request (login itself is already refused for deleted accounts).
				.addFilterBefore(new ActiveAccountFilter(userRepository), TwoFactorRateLimitFilter.class)
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
				.requestCache(cache -> cache.disable())
				// Sent on HTTPS responses only (Spring's own rule). One year, but without
				// includeSubDomains: other subdomains of the pilot's domain may not serve HTTPS, and
				// that flag would lock browsers out of them for a year (see docs/DEPLOY.md).
				.headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts
						.includeSubDomains(false)
						.maxAgeInSeconds(31_536_000)));
		return http.build();
	}

}
