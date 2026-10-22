package ua.kidlearn.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The local dev stack (infra/docker-compose.yml) as a file: what it must and mustn't say. Run from
 * the backend module, like the rest of the suite.
 */
class DevComposeTest {

	private static String devCompose() throws IOException {
		return Files.readString(Path.of("..", "infra", "docker-compose.yml"));
	}

	@Test
	void containersAreNamedByTheProjectSoASecondStackCanRunBesideIt() throws IOException {
		// QA1-001: fixed container names made `docker compose -p <other>` fail with a name conflict.
		assertThat(devCompose()).doesNotContain("container_name");
	}


	@Test
	void everyPortListensOnLoopbackOnly() throws IOException {
		// QA1-009: "5432:5432" listens on every interface — Postgres, MinIO and Mailpit of a dev
		// machine were reachable from the café's or the office's Wi-Fi.
		List<String> mappings = devCompose().lines()
				.map(String::strip)
				.filter(line -> line.matches("- \"[0-9.:]+\""))
				.toList();
		assertThat(mappings).isNotEmpty().allMatch(line -> line.startsWith("- \"127.0.0.1:"));
	}

}
