package ua.kidlearn.consents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

	long countByChildIdAndType(UUID childId, String type);

	boolean existsByChildIdAndTypeAndRevokedAtIsNull(UUID childId, String type);

	Optional<Consent> findFirstByChildIdAndTypeAndRevokedAtIsNull(UUID childId, String type);

	List<Consent> findByChildIdOrderByGrantedAtAsc(UUID childId);

}
