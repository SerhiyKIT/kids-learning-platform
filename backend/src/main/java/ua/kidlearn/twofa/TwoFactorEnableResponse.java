package ua.kidlearn.twofa;

import java.util.List;

/** {@code backupCodes} is the plaintext — returned exactly once, never retrievable again. */
public record TwoFactorEnableResponse(List<String> backupCodes) {
}
