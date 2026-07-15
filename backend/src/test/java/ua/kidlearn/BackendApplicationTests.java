package ua.kidlearn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// dev profile needed since context now includes a JPA datasource (see infra/docker-compose.yml).
@SpringBootTest
@ActiveProfiles("dev")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
