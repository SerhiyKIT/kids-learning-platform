package ua.kidlearn.account;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** GET /api/me/export: the account itself, the consents it gave, and a full export of each of its children. */
public record AccountDataExport(int schemaVersion, Instant exportedAt, Account account,
		List<GivenConsent> consentsGiven, List<ChildDataExport> children) {

	public record Account(String email, String displayName, String role, String locale, Instant createdAt,
			Instant emailVerifiedAt) {
	}

	public record GivenConsent(UUID childId, String type, Instant grantedAt, Instant revokedAt) {
	}

}
