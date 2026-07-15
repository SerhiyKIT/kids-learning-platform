-- Core relational schema, per docs/Модель_даних.md.
-- RLS added in a later migration once session-context propagation is defined.

-- Shared trigger: keeps updated_at current on row update.
CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 1. modules — reference list of learning modules
-- ============================================================
CREATE TABLE modules (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code       text NOT NULL UNIQUE,
    name       text NOT NULL,
    sort_order int  NOT NULL
);

-- ============================================================
-- 2. users — adult accounts (parents, teachers, admins)
-- ============================================================
CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         text NOT NULL UNIQUE,
    password_hash text NULL,
    role          text NOT NULL CONSTRAINT chk_users_role CHECK (role IN ('parent', 'teacher', 'admin')),
    display_name  text NOT NULL,
    locale        text NOT NULL DEFAULT 'uk',
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz NULL
);

CREATE TRIGGER trg_users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- 3. children — child profiles (no email/password; minimized data)
-- ============================================================
CREATE TABLE children (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name  text NOT NULL,
    birth_year    smallint NOT NULL,
    avatar_id     text NOT NULL,
    pin_code_hash text NULL,
    created_by    uuid NOT NULL REFERENCES users (id),
    status        text NOT NULL CONSTRAINT chk_children_status
        CHECK (status IN ('pending_consent', 'active', 'suspended', 'deleted')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz NULL
);

CREATE INDEX idx_children_created_by ON children (created_by);

-- ============================================================
-- 4. parent_child — many-to-many parents <-> children
-- ============================================================
CREATE TABLE parent_child (
    parent_id  uuid NOT NULL REFERENCES users (id),
    child_id   uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    relation   text NOT NULL CONSTRAINT chk_parent_child_relation
        CHECK (relation IN ('mother', 'father', 'guardian')),
    is_primary boolean NOT NULL DEFAULT false,
    PRIMARY KEY (parent_id, child_id)
);

CREATE INDEX idx_parent_child_child_id ON parent_child (child_id);

-- ============================================================
-- 5. consents — parental consent records
-- ============================================================
CREATE TABLE consents (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id    uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    parent_id   uuid NOT NULL REFERENCES users (id),
    type        text NOT NULL CONSTRAINT chk_consents_type
        CHECK (type IN ('account', 'microphone', 'assistant_level2', 'style_personalization')),
    granted_at  timestamptz NOT NULL,
    revoked_at  timestamptz NULL
);

CREATE INDEX idx_consents_child_id ON consents (child_id);
CREATE INDEX idx_consents_parent_id ON consents (parent_id);

-- ============================================================
-- 6. groups — teacher-owned learning groups
-- ============================================================
CREATE TABLE groups (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id uuid NOT NULL REFERENCES users (id),
    name       text NOT NULL,
    join_code  text NOT NULL UNIQUE,
    is_active  boolean NOT NULL DEFAULT true
);

CREATE INDEX idx_groups_teacher_id ON groups (teacher_id);

-- ============================================================
-- 7. group_members — children in groups
-- ============================================================
CREATE TABLE group_members (
    child_id  uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    group_id  uuid NOT NULL REFERENCES groups (id),
    joined_at timestamptz NOT NULL,
    PRIMARY KEY (child_id, group_id)
);

CREATE INDEX idx_group_members_group_id ON group_members (group_id);

-- ============================================================
-- 8. invitations — parent_to_group / teacher_to_parent flows
-- ============================================================
CREATE TABLE invitations (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    type       text NOT NULL CONSTRAINT chk_invitations_type
        CHECK (type IN ('parent_to_group', 'teacher_to_parent')),
    child_id   uuid NULL REFERENCES children (id) ON DELETE CASCADE,
    group_id   uuid NULL REFERENCES groups (id),
    token      text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz NULL
);

CREATE INDEX idx_invitations_child_id ON invitations (child_id);
CREATE INDEX idx_invitations_group_id ON invitations (group_id);

-- ============================================================
-- 9. lessons — lesson container (current_version_id FK added in step 11)
-- ============================================================
CREATE TABLE lessons (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id           uuid NOT NULL REFERENCES modules (id),
    owner_teacher_id    uuid NULL REFERENCES users (id),
    title               text NOT NULL,
    current_version_id  uuid NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    deleted_at          timestamptz NULL
);

CREATE INDEX idx_lessons_module_id ON lessons (module_id);
CREATE INDEX idx_lessons_owner_teacher_id ON lessons (owner_teacher_id);

-- ============================================================
-- 10. lesson_versions — versioned JSON scenarios
-- ============================================================
CREATE TABLE lesson_versions (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id     uuid NOT NULL REFERENCES lessons (id),
    version_no    int  NOT NULL,
    scenario      jsonb NOT NULL,
    generated_by  text NOT NULL CONSTRAINT chk_lesson_versions_generated_by
        CHECK (generated_by IN ('ai', 'human', 'ai_edited')),
    ai_model      text NULL,
    status        text NOT NULL CONSTRAINT chk_lesson_versions_status
        CHECK (status IN ('draft', 'generated', 'auto_validated', 'rejected_auto', 'approved', 'published', 'archived')),
    approved_by   uuid NULL REFERENCES users (id),
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_lesson_versions_lesson_id ON lesson_versions (lesson_id);
CREATE INDEX idx_lesson_versions_approved_by ON lesson_versions (approved_by);

-- ============================================================
-- 11. lessons.current_version_id -> lesson_versions
--     Resolves the lessons <-> lesson_versions circular reference.
-- ============================================================
ALTER TABLE lessons
    ADD CONSTRAINT fk_lessons_current_version
        FOREIGN KEY (current_version_id) REFERENCES lesson_versions (id) ON DELETE RESTRICT;

CREATE INDEX idx_lessons_current_version_id ON lessons (current_version_id);

-- ============================================================
-- 12. audio_assets — deduplicated TTS cache (no child data)
-- ============================================================
CREATE TABLE audio_assets (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    text_hash   text NOT NULL UNIQUE,
    text        text NOT NULL,
    provider    text NOT NULL,
    voice_id    text NOT NULL,
    file_url    text NOT NULL,
    duration_ms int  NOT NULL
);

-- ============================================================
-- 13. themes — approved asset library style themes
-- ============================================================
CREATE TABLE themes (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name            text NOT NULL,
    palette         jsonb NOT NULL,
    guide_character text NOT NULL,
    status          text NOT NULL CONSTRAINT chk_themes_status CHECK (status IN ('draft', 'approved'))
);

-- ============================================================
-- 14. child_style_prefs — child style preferences (needs style_personalization consent)
-- ============================================================
CREATE TABLE child_style_prefs (
    child_id uuid PRIMARY KEY REFERENCES children (id) ON DELETE CASCADE,
    answers  jsonb NOT NULL,
    theme_id uuid NULL REFERENCES themes (id)
);

CREATE INDEX idx_child_style_prefs_theme_id ON child_style_prefs (theme_id);

-- ============================================================
-- 15. lesson_assignments — a lesson version assigned to a group or a child
-- ============================================================
CREATE TABLE lesson_assignments (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_version_id  uuid NOT NULL REFERENCES lesson_versions (id) ON DELETE RESTRICT,
    group_id           uuid NULL REFERENCES groups (id),
    child_id           uuid NULL REFERENCES children (id) ON DELETE CASCADE,
    assigned_by        uuid NOT NULL REFERENCES users (id),
    available_from     timestamptz NULL,
    due_at             timestamptz NULL,
    CONSTRAINT chk_lesson_assignments_target CHECK (group_id IS NOT NULL OR child_id IS NOT NULL)
);

CREATE INDEX idx_lesson_assignments_lesson_version_id ON lesson_assignments (lesson_version_id);
CREATE INDEX idx_lesson_assignments_group_id ON lesson_assignments (group_id);
CREATE INDEX idx_lesson_assignments_child_id ON lesson_assignments (child_id);
CREATE INDEX idx_lesson_assignments_assigned_by ON lesson_assignments (assigned_by);

-- ============================================================
-- 16. lesson_attempts — a child's run through a lesson version
-- ============================================================
CREATE TABLE lesson_attempts (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id           uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    assignment_id      uuid NULL REFERENCES lesson_assignments (id) ON DELETE SET NULL,
    lesson_version_id  uuid NOT NULL REFERENCES lesson_versions (id) ON DELETE RESTRICT,
    started_at         timestamptz NOT NULL,
    completed_at       timestamptz NULL,
    result             text NULL CONSTRAINT chk_lesson_attempts_result CHECK (result IN ('completed', 'abandoned')),
    score              numeric NULL
);

CREATE INDEX idx_lesson_attempts_child_id ON lesson_attempts (child_id);
CREATE INDEX idx_lesson_attempts_assignment_id ON lesson_attempts (assignment_id);
CREATE INDEX idx_lesson_attempts_lesson_version_id ON lesson_attempts (lesson_version_id);

-- ============================================================
-- 17. scene_answers — every answer a child gives, per scene
-- ============================================================
CREATE TABLE scene_answers (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id     uuid NOT NULL REFERENCES lesson_attempts (id) ON DELETE CASCADE,
    scene_key      text NOT NULL,
    try_no         int  NOT NULL,
    chosen_option  text NOT NULL,
    is_correct     boolean NOT NULL,
    hints_used     smallint NOT NULL,
    answered_at    timestamptz NOT NULL
);

CREATE INDEX idx_scene_answers_attempt_id ON scene_answers (attempt_id);

-- ============================================================
-- 18. assistant_dialogs — level-2 assistant free-question log
-- ============================================================
CREATE TABLE assistant_dialogs (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id       uuid NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    attempt_id     uuid NOT NULL REFERENCES lesson_attempts (id) ON DELETE CASCADE,
    question_text  text NOT NULL,
    answer_text    text NOT NULL,
    filter_passed  boolean NOT NULL,
    fallback_used  boolean NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_assistant_dialogs_child_id ON assistant_dialogs (child_id);
CREATE INDEX idx_assistant_dialogs_attempt_id ON assistant_dialogs (attempt_id);
