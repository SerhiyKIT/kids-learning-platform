-- Style personalization (docs/Формат_подання_уроків_та_ШІ.md: a theme is a palette, guide accessory
-- and world decor of the SAME asset library — never generated graphics). A generated theme is shared
-- by every child with the same answers (preference_key, at most 72) and is moderated like a lesson:
-- draft -> approved | rejected. V2 stays untouched; this only widens and extends.
ALTER TABLE themes DROP CONSTRAINT chk_themes_status;
ALTER TABLE themes ADD CONSTRAINT chk_themes_status CHECK (status IN ('draft', 'approved', 'rejected'));

ALTER TABLE themes
    -- "colorId|worldId|moodId"; NULL only for theme_default.
    ADD COLUMN preference_key  text NULL CONSTRAINT uq_themes_preference_key UNIQUE,
    ADD COLUMN world_id        text NULL,
    ADD COLUMN guide_accessory text NOT NULL DEFAULT 'none',
    ADD COLUMN generated_by    text NOT NULL DEFAULT 'human'
        CONSTRAINT chk_themes_generated_by CHECK (generated_by IN ('ai', 'human')),
    ADD COLUMN ai_model        text NULL,
    ADD COLUMN created_at      timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN approved_by     uuid NULL REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN approved_at     timestamptz NULL,
    ADD COLUMN reject_reason   text NULL;

-- Which approved theme the guide has already announced to the child ("look, a new room!"), so the
-- announcement happens once. Deleted with the rest of the row on consent revocation.
ALTER TABLE child_style_prefs ADD COLUMN theme_seen_id uuid NULL;

-- The base theme: today's child-mode palette (frontend globals.css --color-kid-*), plus the stage
-- gradient ends and the guide's colours that were literals in the components until now.
INSERT INTO themes (id, name, palette, guide_character, status, preference_key, world_id, guide_accessory,
                    generated_by, approved_at)
VALUES ('00000000-0000-4000-8000-000000000001', 'theme_default',
        '{"canvas": "#ffe9c9", "bg-top": "#fff3dd", "bg-bottom": "#ffd39c", "sun": "#ffc13b",
          "sun-deep": "#7a4e0b", "teal": "#2fb3a3", "ink": "#5a4322", "ink-soft": "#8a6a3c",
          "pip": "#e8d3ae", "glow": "#ffd98a", "correct": "#8ede9f", "correct-surface": "#eafaee",
          "scroll": "#f0c78a", "guide-main": "#b9e7dd", "guide-accent": "#a9dfd4"}',
        'rabbit', 'approved', NULL, NULL, 'none', 'human', now());
