package ua.kidlearn.themes;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChildStylePrefsRepository extends JpaRepository<ChildStylePrefs, UUID> {

	/** On approval: every child whose answers match the theme's preference key gets it. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE child_style_prefs SET theme_id = :themeId
			WHERE (answers ->> 'colorId') || '|' || (answers ->> 'worldId') || '|' || (answers ->> 'moodId') = :preferenceKey
			""", nativeQuery = true)
	int assignTheme(UUID themeId, String preferenceKey);

}
