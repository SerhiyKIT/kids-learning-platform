package ua.kidlearn.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditEvent, UUID> {

	List<AuditEvent> findByActorIdAndTargetTypeAndTargetId(UUID actorId, String targetType, UUID targetId);

	/** Newest first, simple limit/offset paging; each filter is applied only when non-null. */
	@Query(value = """
			SELECT * FROM audit_log
			WHERE (:targetType IS NULL OR target_type = :targetType)
			  AND (:targetId IS NULL OR target_id = :targetId)
			  AND (:actorId IS NULL OR actor_id = :actorId)
			ORDER BY created_at DESC
			LIMIT :limit OFFSET :offset
			""", nativeQuery = true)
	List<AuditEvent> findFiltered(@Param("targetType") String targetType, @Param("targetId") UUID targetId,
			@Param("actorId") UUID actorId, @Param("limit") int limit, @Param("offset") int offset);

}
