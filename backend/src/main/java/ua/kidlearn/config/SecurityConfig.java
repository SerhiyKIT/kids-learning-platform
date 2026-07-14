package ua.kidlearn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Placeholder security setup. Only permits actuator health/info so the
 * skeleton boots; everything else requires authentication.
 *
 * TODO: add roles (parent/teacher/admin), method-level @PreAuthorize rules,
 * bcrypt/argon2 password hashing, and Google OAuth2 login.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				.anyRequest().authenticated());
		return http.build();
	}

}
