package ua.kidlearn.themes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps exactly to the {@code child_style_prefs} table. Exists only while the parent's
 * style_personalization consent does (revocation deletes the row; so does deleting the child).
 */
@Entity
@Table(name = "child_style_prefs")
public class ChildStylePrefs {

	@Id
	@Column(name = "child_id")
	private UUID childId;

	// {"colorId": …, "worldId": …, "moodId": …} — catalog ids only.
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String answers;

	// The approved theme the child currently sees; null = theme_default.
	@Column(name = "theme_id")
	private UUID themeId;

	@Column(name = "theme_seen_id")
	private UUID themeSeenId;

	protected ChildStylePrefs() {
		// JPA
	}

	ChildStylePrefs(UUID childId) {
		this.childId = childId;
	}

	public UUID getChildId() {
		return childId;
	}

	public String getAnswers() {
		return answers;
	}

	public UUID getThemeId() {
		return themeId;
	}

	public UUID getThemeSeenId() {
		return themeSeenId;
	}

	void setAnswers(String answersJson) {
		this.answers = answersJson;
	}

	void setThemeId(UUID themeId) {
		this.themeId = themeId;
	}

	void markSeen() {
		this.themeSeenId = themeId;
	}

}
