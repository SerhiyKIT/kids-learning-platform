package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.scenario.ScenarioFixtures;

class VoiceLinesTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@Test
	void hashIsSha256OfVoiceIdNewlineTextAsLowercaseHex() throws Exception {
		String expected = HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest("default\nПривіт".getBytes(StandardCharsets.UTF_8)));

		assertThat(VoiceLines.hash("default", "Привіт")).isEqualTo(expected).matches(VoiceLines.TEXT_HASH);
	}

	@Test
	void collectsEveryDistinctVoiceLineTextInTheScenario() {
		var texts = VoiceLines.collectTexts(objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO));

		assertThat(texts).contains("Remember to look both ways", "Watch me cross the street safely", "Look both ways")
				.doesNotHaveDuplicates()
				.doesNotContain("Crossing the street"); // the title is not a voice line
	}

	@Test
	void objectKeyAcceptsOnlyValidatedHashes() {
		String hash = "a".repeat(64);
		assertThat(VoiceLines.objectKey(hash)).isEqualTo("tts/" + hash);
		assertThatThrownBy(() -> VoiceLines.objectKey("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VoiceLines.objectKey("A".repeat(64))).isInstanceOf(IllegalArgumentException.class);
	}

}
