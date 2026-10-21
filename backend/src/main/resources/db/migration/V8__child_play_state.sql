-- Server-side play session for parental control (docs/UX_гайд_дитячого_режиму.md §6). One row per
-- child, created lazily on the first lesson start or settings change. Only what the limit needs:
-- no per-session history is kept. Deleted together with the child (right to be forgotten).
CREATE TABLE child_play_state (
    child_id              uuid PRIMARY KEY REFERENCES children (id) ON DELETE CASCADE,
    -- NULL = no limit. The allowed values (10/15/20/30) are enforced by the API.
    session_limit_minutes smallint NULL DEFAULT 15,
    session_started_at    timestamptz NULL,
    last_activity_at      timestamptz NULL,
    rest_until            timestamptz NULL
);
