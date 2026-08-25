package ua.kidlearn.twofa;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * AES/GCM encryption for TOTP secrets at rest (never store the raw secret). Ciphertext is stored
 * as base64(iv || ciphertext+tag) — a fresh random IV per encryption, prepended so decryption
 * doesn't need a separate column for it.
 */
@Service
class TwofaEncryptionService {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final SecretKeySpec key;

	TwofaEncryptionService(TwofaProperties properties) {
		String encKey = properties.encKey();
		if (encKey == null || encKey.isBlank()) {
			throw new IllegalStateException(
					"app.twofa.enc-key (TWOFA_ENC_KEY) must be set to encrypt admin TOTP secrets");
		}
		this.key = new SecretKeySpec(Base64.getDecoder().decode(encKey), "AES");
	}

	String encrypt(String plaintext) {
		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			RANDOM.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(combined);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to encrypt TOTP secret", e);
		}
	}

	String decrypt(String encoded) {
		try {
			byte[] combined = Base64.getDecoder().decode(encoded);
			byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
			byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to decrypt TOTP secret", e);
		}
	}

}
