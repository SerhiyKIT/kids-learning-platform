package ua.kidlearn.consents;

import java.util.UUID;

/**
 * A parent revoked a consent. Published synchronously inside the revoking transaction, so whatever
 * the consent allowed us to keep is deleted together with the revocation, or not at all.
 */
public record ConsentRevokedEvent(UUID childId, String type) {
}
