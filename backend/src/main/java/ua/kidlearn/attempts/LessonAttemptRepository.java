package ua.kidlearn.attempts;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonAttemptRepository extends JpaRepository<LessonAttempt, UUID> {

	List<LessonAttempt> findByChildIdOrderByStartedAtDesc(UUID childId);

}
