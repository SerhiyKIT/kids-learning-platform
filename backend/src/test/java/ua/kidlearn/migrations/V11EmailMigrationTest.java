package ua.kidlearn.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.kidlearn.support.WebIntegrationTest;

/**
 * V11 on a throwaway schema: migrated to V10, given rows, then V11. Accounts that collide after
 * normalization stop the migration with a clear message (merging is a human decision); otherwise
 * every email is rewritten to the canonical form.
 */
class V11EmailMigrationTest extends WebIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	private final String schema = "v11_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

	@AfterEach
	void dropSchema() {
		jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
	}

	private Flyway flyway(String target) {
		return Flyway.configure().dataSource(dataSource).schemas(schema).locations("classpath:db/migration")
				.target(target).load();
	}

	private void insertUser(String email) {
		jdbc.update("INSERT INTO " + schema + ".users (email, role, display_name) VALUES (?, 'parent', 'X')", email);
	}

	@Test
	void collidingEmailsStopTheMigrationWithoutTouchingAnything() {
		flyway("10").migrate();
		insertUser("Mama@Gmail.com");
		insertUser(" mama@gmail.com");

		assertThatThrownBy(() -> flyway("11").migrate())
				.hasMessageContaining("must be merged by hand")
				.hasMessageContaining("mama@gmail.com (2 accounts)");
		assertThat(jdbc.queryForList("SELECT email FROM " + schema + ".users ORDER BY email", String.class))
				.containsExactly(" mama@gmail.com", "Mama@Gmail.com");
	}

	@Test
	void withoutCollisionsEveryEmailIsNormalizedAndKeptThatWay() {
		flyway("10").migrate();
		insertUser("  Tato@Example.ORG ");
		insertUser("baba@example.org");

		flyway("11").migrate();

		assertThat(jdbc.queryForList("SELECT email FROM " + schema + ".users ORDER BY email", String.class))
				.containsExactly("baba@example.org", "tato@example.org");
		assertThatThrownBy(() -> insertUser("Other@example.org")).hasMessageContaining("chk_users_email_normalized");
	}

}
