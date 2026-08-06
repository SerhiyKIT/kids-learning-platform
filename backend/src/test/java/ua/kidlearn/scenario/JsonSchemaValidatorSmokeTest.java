package ua.kidlearn.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * STEP 0 of scenario validation: com.networknt:json-schema-validator 3.0.6 depends on
 * tools.jackson.* (Jackson 3), matching Boot 4.1's jackson-databind 3.1.4 exactly — so unlike
 * the 2.x line (Jackson-2-only), it resolves and runs without pulling in a conflicting Jackson 2.
 */
class JsonSchemaValidatorSmokeTest {

	@Test
	void resolvesAndValidatesATrivialSchema() {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode schemaNode = mapper.readTree("""
				{
				  "type": "object",
				  "required": ["name"],
				  "properties": { "name": { "type": "string" } }
				}
				""");
		SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);
		Schema schema = registry.getSchema(schemaNode);

		JsonNode valid = mapper.readTree("{\"name\":\"ok\"}");
		JsonNode invalid = mapper.readTree("{}");

		assertThat(schema.validate(valid)).isEmpty();
		List<Error> errors = schema.validate(invalid);
		assertThat(errors).isNotEmpty();
	}

}
