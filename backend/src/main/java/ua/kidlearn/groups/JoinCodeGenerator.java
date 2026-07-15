package ua.kidlearn.groups;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates 8-char join codes from an alphabet with no easily-confused characters. */
@Component
class JoinCodeGenerator {

	// A-Z + 2-9, excluding O/0/I/1.
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int LENGTH = 8;
	private static final SecureRandom RANDOM = new SecureRandom();

	String generate() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

}
