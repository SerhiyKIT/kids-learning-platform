package ua.kidlearn.aipipeline;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioAssetRepository extends JpaRepository<AudioAsset, UUID> {

	Optional<AudioAsset> findByTextHash(String textHash);

	/** One query for a whole scenario: exact-voice hits plus same-text fallbacks. */
	List<AudioAsset> findByTextHashInOrTextIn(Collection<String> textHashes, Collection<String> texts);

}
