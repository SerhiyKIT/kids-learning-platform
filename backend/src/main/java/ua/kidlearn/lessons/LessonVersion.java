package ua.kidlearn.lessons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Maps exactly to the {@code lesson_versions} table (see V2__core_schema.sql). */
@Entity
@Table(name = "lesson_versions")
public class LessonVersion {

	public static final String STATUS_DRAFT = "draft";
	public static final String STATUS_PUBLISHED = "published";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "lesson_id", nullable = false)
	private UUID lessonId;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	// Raw JSON text; Postgres validates/stores it as jsonb, Hibernate never
	// parses it into a Java object graph.
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String scenario;

	@Column(name = "generated_by", nullable = false)
	private String generatedBy;

	@Column(name = "ai_model")
	private String aiModel;

	@Column(nullable = false)
	private String status;

	@Column(name = "approved_by")
	private UUID approvedBy;

	// DB-owned (DEFAULT now()); Hibernate never writes it.
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected LessonVersion() {
		// JPA
	}

	public LessonVersion(UUID lessonId, int versionNo, String scenario, String generatedBy) {
		this.lessonId = lessonId;
		this.versionNo = versionNo;
		this.scenario = scenario;
		this.generatedBy = generatedBy;
		this.status = STATUS_DRAFT;
	}

	public UUID getId() {
		return id;
	}

	public UUID getLessonId() {
		return lessonId;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public String getScenario() {
		return scenario;
	}

	public String getGeneratedBy() {
		return generatedBy;
	}

	public String getAiModel() {
		return aiModel;
	}

	public String getStatus() {
		return status;
	}

	public UUID getApprovedBy() {
		return approvedBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isPublished() {
		return STATUS_PUBLISHED.equals(status);
	}

	public void publish() {
		this.status = STATUS_PUBLISHED;
	}

}
