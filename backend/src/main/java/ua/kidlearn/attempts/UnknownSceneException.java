package ua.kidlearn.attempts;

/** An answer for a scene the attempt's lesson doesn't have (QA1-015). */
class UnknownSceneException extends RuntimeException {

	UnknownSceneException() {
		super("No such scene in the lesson");
	}

}
