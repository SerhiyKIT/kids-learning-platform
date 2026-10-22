package ua.kidlearn.lessons;

/** The same lesson version is already assigned to this group or child (QA1-018). */
class AssignmentExistsException extends RuntimeException {

	AssignmentExistsException() {
		super("Lesson already assigned");
	}

}
