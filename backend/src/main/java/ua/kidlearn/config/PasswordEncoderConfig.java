package ua.kidlearn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Delegating encoder: bcrypt by default, prefixes the hash with "{bcrypt}"
		// so we can switch the default encoding (e.g. to argon2) later with no
		// schema change — password_hash is a plain text column.
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
