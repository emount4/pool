DROP TABLE IF EXISTS visits;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS system_users;

CREATE TABLE system_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_username
        CHECK (
            username = trim(username)
            AND length(username) BETWEEN 3 AND 50
        ),

    CONSTRAINT chk_user_role
        CHECK (role IN ('ADMIN', 'OPERATOR'))
);

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    birth_date DATE NOT NULL,

    CONSTRAINT chk_client_first_name
        CHECK (length(trim(first_name)) >= 2),

    CONSTRAINT chk_client_last_name
        CHECK (length(trim(last_name)) >= 2),

    CONSTRAINT chk_client_phone
        CHECK (length(trim(phone)) > 0),

    CONSTRAINT chk_client_email
        CHECK (email IS NULL OR email LIKE '%_@_%._%'),

    CONSTRAINT chk_client_birth_date
        CHECK (birth_date <= CURRENT_DATE)
);

CREATE TABLE visits (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    start_time TIME NOT NULL,
    duration_minutes INT NOT NULL,
    lane_number INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT,

    CONSTRAINT fk_visit_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id),

    CONSTRAINT fk_visit_created_by
        FOREIGN KEY (created_by)
        REFERENCES system_users(id),

    CONSTRAINT chk_duration
        CHECK (duration_minutes BETWEEN 30 AND 180),

    CONSTRAINT chk_lane
        CHECK (lane_number BETWEEN 1 AND 8),

    CONSTRAINT chk_visit_status
        CHECK (
            status IN (
                'PLANNED',
                'IN_PROGRESS',
                'COMPLETED',
                'CANCELLED'
            )
        )
);

CREATE INDEX idx_visits_client_id ON visits(client_id);
CREATE INDEX idx_visits_visit_date ON visits(visit_date);
CREATE INDEX idx_visits_status ON visits(status);
CREATE INDEX idx_visits_created_by ON visits(created_by);
