-- Admin/teacher data-access audit log for child data (docs/Ролі_та_приватність.md §3, §8).
-- Rows must outlive the child they reference (a deletion's audit trail is the whole point of
-- auditing it), so target_id carries no FK to children — only actor_id references users, and
-- with ON DELETE SET NULL rather than CASCADE, so the log survives the actor's own deletion too.
CREATE TABLE audit_log (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    uuid NULL REFERENCES users (id) ON DELETE SET NULL,
    actor_role  text NOT NULL,
    action      text NOT NULL,
    target_type text NOT NULL,
    target_id   uuid NULL,
    client_ip   text NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_actor_id ON audit_log (actor_id);
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
