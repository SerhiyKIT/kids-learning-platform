package ua.kidlearn.themes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Maps exactly to the {@code themes} table (V2__core_schema.sql + V9__style_themes.sql). */
@Entity
@Table(name = "themes")
public class Theme {

	public static final String STATUS_DRAFT = "draft";
	public static final String STATUS_APPROVED = "approved";
	public static final String STATUS_REJECTED = "rejected";

	/** theme_default (seeded by V9): the child-mode palette everyone has without an approved theme. */
	public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

	private static final String GUIDE_CHARACTER = "rabbit";

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String palette;

	@Column(name = "guide_character", nullable = false)
	private String guideCharacter;

	@Column(nullable = false)
	private String status;

	@Column(name = "preference_key")
	private String preferenceKey;

	@Column(name = "world_id")
	private String worldId;

	@Column(name = "guide_accessory", nullable = false)
	private String guideAccessory;

	@Column(name = "generated_by", nullable = false)
	private String generatedBy;

	@Column(name = "ai_model")
	private String aiModel;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "reject_reason")
	private String rejectReason;

	protected Theme() {
		// JPA
	}

	/** A freshly generated theme: draft if it passed ThemeValidator, rejected (with reason) if not. */
	public static Theme generated(String preferenceKey, String worldId, Instant now) {
		Theme theme = new Theme();
		theme.id = UUID.randomUUID();
		theme.preferenceKey = preferenceKey;
		theme.worldId = worldId;
		theme.guideCharacter = GUIDE_CHARACTER;
		theme.generatedBy = "ai";
		theme.createdAt = now;
		return theme;
	}

	/** Replaces the content with a new generation result (also used to regenerate a rejected theme). */
	void setGeneration(String name, String paletteJson, String guideAccessory, String aiModel, String rejectReason) {
		this.name = name;
		this.palette = paletteJson;
		this.guideAccessory = guideAccessory;
		this.aiModel = aiModel;
		this.rejectReason = rejectReason;
		this.status = rejectReason == null ? STATUS_DRAFT : STATUS_REJECTED;
		this.approvedBy = null;
		this.approvedAt = null;
	}

	void approve(UUID adminId, Instant now) {
		this.status = STATUS_APPROVED;
		this.approvedBy = adminId;
		this.approvedAt = now;
		this.rejectReason = null;
	}

	void reject(String reason) {
		this.status = STATUS_REJECTED;
		this.rejectReason = reason;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPalette() {
		return palette;
	}

	public String getStatus() {
		return status;
	}

	public boolean isApproved() {
		return STATUS_APPROVED.equals(status);
	}

	public String getPreferenceKey() {
		return preferenceKey;
	}

	public String getWorldId() {
		return worldId;
	}

	public String getGuideAccessory() {
		return guideAccessory;
	}

	public String getGeneratedBy() {
		return generatedBy;
	}

	public String getAiModel() {
		return aiModel;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public UUID getApprovedBy() {
		return approvedBy;
	}

	public Instant getApprovedAt() {
		return approvedAt;
	}

	public String getRejectReason() {
		return rejectReason;
	}

}
