package ua.kidlearn.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import ua.kidlearn.users.User;

/** Maps to the {@code user_tokens} table (see V4__auth_email_tokens.sql). */
@Entity
@Table(name = "user_tokens")
public class UserToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Convert(converter = TokenTypeConverter.class)
	@Column(nullable = false)
	private TokenType type;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	// DB-owned (DEFAULT now()); Hibernate never writes it.
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected UserToken() {
		// JPA
	}

	public UserToken(User user, TokenType type, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.type = type;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public TokenType getType() {
		return type;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && expiresAt.isAfter(now);
	}

	public void markUsed() {
		this.usedAt = Instant.now();
	}

}
