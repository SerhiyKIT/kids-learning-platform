package ua.kidlearn.lessons;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	List<Lesson> findByCurrentVersionIdIsNotNull();

	// title isn't unique on its own (see V2__core_schema.sql); scoping by module keeps this a
	// stable idempotency key for callers (e.g. the dev seed) that know both. findFirst..., not
	// findBy..., so a pre-existing duplicate can't blow this up with NonUniqueResultException.
	Optional<Lesson> findFirstByTitleAndModuleId(String title, UUID moduleId);

}
