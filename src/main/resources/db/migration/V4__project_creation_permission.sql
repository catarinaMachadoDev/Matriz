ALTER TABLE app_user ADD COLUMN can_create_projects boolean NOT NULL DEFAULT false;

-- Preserve existing accounts' creation rights except for the member account.
UPDATE app_user SET can_create_projects = true WHERE username <> 'sam';
