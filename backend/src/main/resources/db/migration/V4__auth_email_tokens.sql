-- Email verification and password reset support.
ALTER TABLE users ADD COLUMN email_verified_at timestamptz NULL;

CREATE TABLE user_tokens (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        text NOT NULL CONSTRAINT chk_user_tokens_type
        CHECK (type IN ('email_verification', 'password_reset')),
    token_hash  text NOT NULL UNIQUE, -- SHA-256 of the raw token, never the raw token
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_tokens_user_id ON user_tokens (user_id);
