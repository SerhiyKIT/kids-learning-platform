package ua.kidlearn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Session-based security for the web-first PWA.
 *
 * TODO: Google OAuth2 login, TOTP 2FA for admins, admin data-access audit log,
 * rate limiting on auth endpoints.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/verify-email",
								"/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
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
				.formLogin(form -> form.permitAll())
				.logout(logout -> logout.permitAll())
				// This is a JSON API with no server-rendered pages to redirect to, so
				// unauthenticated access must return 401, not a 302 to /login.
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
		return http.build();
	}

}
