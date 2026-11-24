CREATE TABLE web_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_web_user_username CHECK (
        username = trim(username) AND length(username) BETWEEN 3 AND 50
    ),
    CONSTRAINT chk_web_user_display_name CHECK (length(trim(display_name)) > 0)
);

CREATE UNIQUE INDEX uq_web_users_username ON web_users (lower(username));

CREATE TABLE web_user_roles (
    user_id UUID NOT NULL REFERENCES web_users (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,

    PRIMARY KEY (user_id, role),
    CONSTRAINT chk_web_user_role CHECK (role IN ('VIEWER', 'OPERATOR', 'ADMIN'))
);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES web_users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT chk_refresh_session_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_sessions_user_id ON refresh_sessions (user_id);
