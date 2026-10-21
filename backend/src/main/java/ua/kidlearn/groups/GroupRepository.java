package ua.kidlearn.groups;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, UUID> {

	List<Group> findByTeacherId(UUID teacherId);

	Optional<Group> findByJoinCodeAndIsActiveTrue(String joinCode);

	Optional<Group> findByJoinCode(String joinCode);

	boolean existsByJoinCode(String joinCode);

	/** {teacherId, groupCount} rows for the given teachers (teachers with no groups are absent). */
	// Native: "Group" is a reserved word in JPQL.
	@Query(value = "SELECT teacher_id, COUNT(*) FROM groups WHERE teacher_id IN (:teacherIds) GROUP BY teacher_id",
			nativeQuery = true)
	List<Object[]> countByTeacherIds(@Param("teacherIds") Collection<UUID> teacherIds);

}
