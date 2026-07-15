package ua.kidlearn.lessons;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonVersionRepository extends JpaRepository<LessonVersion, UUID> {

	Optional<LessonVersion> findFirstByLessonIdOrderByVersionNoDesc(UUID lessonId);

}
