package ua.kidlearn.auth;

/** Resending an invitation the teacher has already accepted (they set a password and are active). */
public class InviteAlreadyAcceptedException extends RuntimeException {

	public InviteAlreadyAcceptedException() {
		super("Invitation already accepted");
	}

}
