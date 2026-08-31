ALTER TABLE app_user
    ADD COLUMN role VARCHAR(32);

UPDATE app_user
SET role = 'USER'
WHERE role IS NULL;

ALTER TABLE app_user
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE app_user
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN'));
