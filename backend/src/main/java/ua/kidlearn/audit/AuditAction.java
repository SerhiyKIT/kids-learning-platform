package ua.kidlearn.audit;

/** Recorded audit_log.action values for the child-data-access surface. */
public final class AuditAction {

	public static final String VIEW_CHILD_HISTORY = "VIEW_CHILD_HISTORY";
	public static final String VIEW_GROUP_RESULTS = "VIEW_GROUP_RESULTS";
	public static final String DELETE_CHILD = "DELETE_CHILD";
	public static final String APPROVE_VERSION = "APPROVE_VERSION";
	public static final String REJECT_VERSION = "REJECT_VERSION";
	public static final String PUBLISH_VERSION = "PUBLISH_VERSION";

	private AuditAction() {
	}

}
