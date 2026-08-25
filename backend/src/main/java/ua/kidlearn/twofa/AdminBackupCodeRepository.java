package ua.kidlearn.twofa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminBackupCodeRepository extends JpaRepository<AdminBackupCode, UUID> {

	List<AdminBackupCode> findByUserIdAndUsedAtIsNull(UUID userId);

}
