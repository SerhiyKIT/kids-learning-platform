package ua.kidlearn.users;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps exactly to the {@code users} table (see V2__core_schema.sql). ddl-auto
 * stays {@code validate}, so this entity must never drift from the DB schema.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Convert(converter = RoleConverter.class)
	@Column(nullable = false)
	private Role role;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(nullable = false)
	private String locale;

	// created_at/updated_at are owned by the DB (DEFAULT now() / set_updated_at()
	// trigger); Hibernate never writes them.
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected User() {
		// JPA
	}

	public User(String email, String passwordHash, Role role, String displayName, String locale) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.displayName = displayName;
		this.locale = locale;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getLocale() {
		return locale;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

}
