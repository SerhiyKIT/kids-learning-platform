package ua.kidlearn.kidplay;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChildPlayStateRepository extends JpaRepository<ChildPlayState, UUID> {

	/** Lazily creates the row with the DB defaults; a concurrent creator simply wins. */
	@Modifying
	@Query(value = "INSERT INTO child_play_state (child_id) VALUES (:childId) ON CONFLICT (child_id) DO NOTHING",
			nativeQuery = true)
	void insertIfAbsent(UUID childId);

	/** Row-locked read, so two tabs starting/completing at once can't both slip past the limit. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ChildPlayState s WHERE s.childId = :childId")
	Optional<ChildPlayState> findForUpdate(UUID childId);

}
