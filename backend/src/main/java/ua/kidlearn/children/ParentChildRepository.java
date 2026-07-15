package ua.kidlearn.children;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentChildRepository extends JpaRepository<ParentChild, ParentChildId> {

	List<ParentChild> findById_ParentId(UUID parentId);

	boolean existsById_ParentIdAndId_ChildId(UUID parentId, UUID childId);

	long countById_ParentId(UUID parentId);

}
