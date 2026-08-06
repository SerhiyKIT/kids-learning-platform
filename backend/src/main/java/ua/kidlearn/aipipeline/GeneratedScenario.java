package ua.kidlearn.aipipeline;

import tools.jackson.databind.JsonNode;

/** Raw LLM output before validation: the scenario JSON plus the model id that produced it. */
public record GeneratedScenario(JsonNode scenario, String modelId) {
}
