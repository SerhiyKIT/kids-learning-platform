package ua.kidlearn.lessons;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonVersionRepository extends JpaRepository<LessonVersion, UUID> {

	Optional<LessonVersion> findFirstByLessonIdOrderByVersionNoDesc(UUID lessonId);

	List<LessonVersion> findByStatusOrderByCreatedAtAsc(String status);

}
