package ua.kidlearn.attempts;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneAnswerRepository extends JpaRepository<SceneAnswer, UUID> {

	List<SceneAnswer> findByAttemptId(UUID attemptId);

}
