package ua.kidlearn.groups;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, UUID> {

	List<Group> findByTeacherId(UUID teacherId);

	Optional<Group> findByJoinCodeAndIsActiveTrue(String joinCode);

	Optional<Group> findByJoinCode(String joinCode);

	boolean existsByJoinCode(String joinCode);

}
