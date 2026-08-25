package ua.kidlearn.twofa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param encKey base64-encoded AES-256 key (32 raw bytes) encrypting admin TOTP secrets at rest
 * — see {@link TwofaEncryptionService}. Set via the {@code TWOFA_ENC_KEY} env var. Blank/unset
 * here in the base config; {@code application-dev.yml} supplies a documented dev-only fallback so
 * local dev/tests work without it, while {@code application-prod.yml} requires it to be set (no
 * default there — the app refuses to start rather than risk running with no key at all).
 */
@ConfigurationProperties(prefix = "app.twofa")
public record TwofaProperties(String encKey) {
}
