package ua.kidlearn.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	/** Exact match on the stored (already normalized) email — use {@link #findActiveByEmail}. */
	Optional<User> findByEmailAndDeletedAtIsNull(String email);

	/** The active account for an email as typed by anyone (any case, surrounding spaces). */
	default Optional<User> findActiveByEmail(String email) {
		return findByEmailAndDeletedAtIsNull(EmailAddress.normalize(email));
	}

	boolean existsByRoleAndDeletedAtIsNull(Role role);

	/** Exact match on the stored (already normalized) email — use {@link #existsAnyByEmail}. */
	boolean existsByEmail(String email);

	/** Any row at all, deleted or not: users.email is unique across every row. */
	default boolean existsAnyByEmail(String email) {
		return existsByEmail(EmailAddress.normalize(email));
	}

	/** Includes soft-deleted rows — callers decide how to present them. */
	List<User> findByRoleOrderByCreatedAtDescIdDesc(Role role);

}
