package ua.kidlearn;

import org.junit.jupiter.api.Test;
import ua.kidlearn.support.WebIntegrationTest;

// dev profile needed since context now includes a JPA datasource (see infra/docker-compose.yml).
class BackendApplicationTests extends WebIntegrationTest {

	@Test
	void contextLoads() {
	}

}
