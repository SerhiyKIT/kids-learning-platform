package ua.kidlearn.children;

import java.time.Instant;
import ua.kidlearn.consents.Consent;

/** GET /api/children/{id}/consents — every consent the parent gave for this child, with revocations. */
public record ConsentEntry(String type, Instant grantedAt, Instant revokedAt) {

	static ConsentEntry from(Consent consent) {
		return new ConsentEntry(consent.getType(), consent.getGrantedAt(), consent.getRevokedAt());
	}

}
