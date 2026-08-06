package ua.kidlearn.aipipeline;

/** Provider-abstracted lesson-scenario generation (Claude / GPT / Gemini — pending the Ukrainian-quality test). */
public interface LlmProvider {

	GeneratedScenario generate(GenerationRequest request);

}
