ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(255);

UPDATE app_user
SET password_hash = '!disabled-user!'
WHERE password_hash IS NULL;

UPDATE app_user
SET enabled = FALSE
WHERE password_hash = '!disabled-user!';

ALTER TABLE app_user
    ALTER COLUMN password_hash SET NOT NULL;

ALTER TABLE app_user
    ALTER COLUMN enabled SET DEFAULT TRUE;
