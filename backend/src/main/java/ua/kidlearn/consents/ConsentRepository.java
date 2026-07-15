package ua.kidlearn.consents;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

	long countByChildIdAndType(UUID childId, String type);

}
