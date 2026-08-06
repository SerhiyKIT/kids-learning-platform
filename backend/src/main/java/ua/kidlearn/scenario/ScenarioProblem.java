package ua.kidlearn.scenario;

/** One validation failure: a JSON-pointer path into the scenario and a human-readable message. */
public record ScenarioProblem(String path, String message) {
}
