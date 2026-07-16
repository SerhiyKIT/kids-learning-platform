package ua.kidlearn.aipipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Maps exactly to the {@code audio_assets} table (see V2__core_schema.sql). */
@Entity
@Table(name = "audio_assets")
public class AudioAsset {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "text_hash", nullable = false, unique = true)
	private String textHash;

	@Column(nullable = false)
	private String text;

	@Column(nullable = false)
	private String provider;

	@Column(name = "voice_id", nullable = false)
	private String voiceId;

	@Column(name = "file_url", nullable = false)
	private String fileUrl;

	@Column(name = "duration_ms", nullable = false)
	private int durationMs;

	protected AudioAsset() {
		// JPA
	}

	public AudioAsset(String textHash, String text, String provider, String voiceId, String fileUrl,
			int durationMs) {
		this.textHash = textHash;
		this.text = text;
		this.provider = provider;
		this.voiceId = voiceId;
		this.fileUrl = fileUrl;
		this.durationMs = durationMs;
	}

	public UUID getId() {
		return id;
	}

	public String getTextHash() {
		return textHash;
	}

	public String getText() {
		return text;
	}

	public String getProvider() {
		return provider;
	}

	public String getVoiceId() {
		return voiceId;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public int getDurationMs() {
		return durationMs;
	}

}
