package ua.kidlearn.themes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, UUID> {

	Optional<Theme> findByPreferenceKey(String preferenceKey);

	boolean existsByPreferenceKey(String preferenceKey);

	List<Theme> findByStatusAndPreferenceKeyIsNotNullOrderByCreatedAtDesc(String status);

}
