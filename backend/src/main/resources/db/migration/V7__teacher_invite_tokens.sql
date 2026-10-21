-- Admin-issued teacher invitations reuse user_tokens (hashed, single-use, expiring) under a new
-- type. Only widens the CHECK; V4 itself stays untouched.
ALTER TABLE user_tokens DROP CONSTRAINT chk_user_tokens_type;
ALTER TABLE user_tokens ADD CONSTRAINT chk_user_tokens_type
    CHECK (type IN ('email_verification', 'password_reset', 'teacher_invite'));
