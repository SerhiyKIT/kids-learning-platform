package ua.kidlearn.lessons;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, UUID> {

	Optional<Module> findByCode(String code);

}
