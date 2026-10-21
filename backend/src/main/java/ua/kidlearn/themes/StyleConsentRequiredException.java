package ua.kidlearn.themes;

/** Saving style answers without the parent's style_personalization consent. */
public class StyleConsentRequiredException extends RuntimeException {

	public StyleConsentRequiredException() {
		super("style_personalization consent required");
	}

}
