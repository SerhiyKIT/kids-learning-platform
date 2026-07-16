package ua.kidlearn.aipipeline;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioAssetRepository extends JpaRepository<AudioAsset, UUID> {

	Optional<AudioAsset> findByTextHash(String textHash);

}
