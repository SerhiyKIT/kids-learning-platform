package ua.kidlearn.children;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, UUID> {
}
