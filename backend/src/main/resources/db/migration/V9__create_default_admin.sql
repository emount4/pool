INSERT INTO web_users (
    username, display_name, password_hash, active, password_change_required
)
VALUES (
    'admin',
    'Администратор',
    '$2a$10$icQB72RJmqxOoohnnXCCJOd1mK59swtCKC.zerFamppp7zYRRSGCK',
    TRUE,
    FALSE
)
ON CONFLICT (lower(username)) DO UPDATE
SET username = 'admin',
    password_hash = EXCLUDED.password_hash,
    active = TRUE,
    password_change_required = FALSE;

INSERT INTO web_user_roles (user_id, role)
SELECT id, 'ADMIN' FROM web_users WHERE username = 'admin'
ON CONFLICT DO NOTHING;

UPDATE refresh_sessions
SET revoked_at = CURRENT_TIMESTAMP
WHERE user_id = (SELECT id FROM web_users WHERE username = 'admin')
    AND revoked_at IS NULL;
