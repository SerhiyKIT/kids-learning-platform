package ua.kidlearn.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

}
