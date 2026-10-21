-- Emails are stored normalized (EmailAddress.normalize: surrounding whitespace dropped, lower case),
-- so "Mama@Gmail.com " and "mama@gmail.com" can't become two accounts.
--
-- 1) Refuse to run if existing rows would collide after normalization. Merging two accounts (whose
--    children, consents and history belong to whom?) is a human decision — see docs/DEPLOY.md
--    «Дублікати email». Nothing is merged or deleted automatically.
DO $$
DECLARE
    duplicates text;
BEGIN
    SELECT string_agg(normalized || ' (' || n || ' accounts)', ', ')
    INTO duplicates
    FROM (SELECT lower(btrim(email, E' \t\r\n')) AS normalized, count(*) AS n
          FROM users
          GROUP BY 1
          HAVING count(*) > 1) d;
    IF duplicates IS NOT NULL THEN
        RAISE EXCEPTION 'V11: emails that differ only by case/whitespace must be merged by hand first (docs/DEPLOY.md, «Дублікати email»): %', duplicates;
    END IF;
END $$;

-- 2) Store every email in the canonical form.
UPDATE users SET email = lower(btrim(email, E' \t\r\n')) WHERE email <> lower(btrim(email, E' \t\r\n'));

-- 3) Keep it that way. A CHECK on the canonical form + the existing plain UNIQUE(email), rather
--    than a unique index on lower(email): every lookup stays a simple equality on the indexed
--    column, and a write that skipped normalization (manual SQL, a future bug) fails loudly
--    instead of silently storing a second spelling.
ALTER TABLE users ADD CONSTRAINT chk_users_email_normalized CHECK (email = lower(btrim(email, E' \t\r\n')));
