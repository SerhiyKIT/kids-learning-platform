package ua.kidlearn.lessons;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonAssignmentRepository extends JpaRepository<LessonAssignment, UUID> {

	List<LessonAssignment> findByGroupId(UUID groupId);

}
