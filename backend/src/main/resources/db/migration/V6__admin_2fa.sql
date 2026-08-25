-- Mandatory TOTP 2FA for admin accounts (docs/CONVENTIONS.md; ua.kidlearn.twofa).
ALTER TABLE users ADD COLUMN totp_secret_enc text NULL;
ALTER TABLE users ADD COLUMN totp_enabled_at timestamptz NULL;

-- Credentials, not audit — cascades with the user, unlike audit_log (ON DELETE SET NULL there).
CREATE TABLE admin_backup_codes (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  text NOT NULL,
    used_at    timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_backup_codes_user_id ON admin_backup_codes (user_id);
