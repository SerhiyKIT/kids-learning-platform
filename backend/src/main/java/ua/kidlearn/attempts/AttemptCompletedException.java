package ua.kidlearn.attempts;

/** The attempt is already finished: no more answers, no second completion (QA1-015). */
class AttemptCompletedException extends RuntimeException {

	AttemptCompletedException() {
		super("Attempt already completed");
	}

}
