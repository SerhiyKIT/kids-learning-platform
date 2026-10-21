package ua.kidlearn.themes;

/** The generator couldn't produce a theme (provider down, refusal, no tool call…). */
public class ThemeGenerationException extends RuntimeException {

	public ThemeGenerationException(String message) {
		super(message);
	}

	public ThemeGenerationException(String message, Throwable cause) {
		super(message, cause);
	}

}
