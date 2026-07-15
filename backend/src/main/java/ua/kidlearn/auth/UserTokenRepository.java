package ua.kidlearn.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.kidlearn.users.User;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

	Optional<UserToken> findByTokenHash(String tokenHash);

	Optional<UserToken> findFirstByUserAndTypeOrderByCreatedAtDesc(User user, TokenType type);

	long countByUserAndType(User user, TokenType type);

}
