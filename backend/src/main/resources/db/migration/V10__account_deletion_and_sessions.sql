-- Right to be forgotten for adult accounts (docs/Ролі_та_приватність.md §1, §3): deleting a
-- parent must be a real DELETE, so the parent's foreign keys get explicit behaviour.
--   children.created_by  → NULL: a child kept by a second parent outlives its creator.
--   parent_child.parent_id → CASCADE: the link is the deleted parent's own data.
--   consents.parent_id   → NULL: the record that consent was given for a child who stays with
--                          another parent is kept, de-identified.
-- Children of a sole parent are deleted first by the application (their own cascade, V2).
ALTER TABLE children ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE children DROP CONSTRAINT children_created_by_fkey;
ALTER TABLE children ADD CONSTRAINT children_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE parent_child DROP CONSTRAINT parent_child_parent_id_fkey;
ALTER TABLE parent_child ADD CONSTRAINT parent_child_parent_id_fkey
    FOREIGN KEY (parent_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE consents ALTER COLUMN parent_id DROP NOT NULL;
ALTER TABLE consents DROP CONSTRAINT consents_parent_id_fkey;
ALTER TABLE consents ADD CONSTRAINT consents_parent_id_fkey
    FOREIGN KEY (parent_id) REFERENCES users (id) ON DELETE SET NULL;

-- "Sign out everywhere" (§2): a session authenticated before this moment is no longer valid.
ALTER TABLE users ADD COLUMN sessions_valid_after timestamptz NULL;
