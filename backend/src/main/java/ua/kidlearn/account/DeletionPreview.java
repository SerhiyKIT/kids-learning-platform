package ua.kidlearn.account;

import java.util.List;
import java.util.UUID;

/**
 * What deleting the account would do to each child, for the confirmation screen.
 *
 * @param deletedCompletely true = this parent is the child's only parent: the child and all their
 *   data go too; false = the child stays with their other parent, only this link is removed
 */
public record DeletionPreview(List<ChildFate> children) {

	public record ChildFate(UUID childId, String displayName, boolean deletedCompletely) {
	}

}
