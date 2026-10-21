package ua.kidlearn.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailAndDeletedAtIsNull(String email);

	boolean existsByRoleAndDeletedAtIsNull(Role role);

	/** Any row at all, deleted or not, case-insensitively: users.email is unique across every row. */
	boolean existsByEmailIgnoreCase(String email);

	/** Includes soft-deleted rows — callers decide how to present them. */
	List<User> findByRoleOrderByCreatedAtDescIdDesc(Role role);

}
