-- QA1-021: one published version per lesson. Publishing a new version marks the previous published
-- one "superseded" — not "archived", which is the moderator's rejection. A superseded version is
-- no longer offered for new assignments, but assignments that already point at it keep working:
-- they name that exact version.
ALTER TABLE lesson_versions DROP CONSTRAINT chk_lesson_versions_status;
ALTER TABLE lesson_versions ADD CONSTRAINT chk_lesson_versions_status
    CHECK (status IN ('draft', 'generated', 'auto_validated', 'rejected_auto', 'approved', 'published', 'archived',
                      'superseded'));

-- Lessons already holding more than one published version keep the current one published.
UPDATE lesson_versions v SET status = 'superseded'
FROM lessons l
WHERE v.lesson_id = l.id AND v.status = 'published' AND l.current_version_id IS DISTINCT FROM v.id;
