CREATE TABLE csv_previews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES web_users(id),
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('NEW_ONLY', 'UPSERT')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE csv_preview_rows (
    preview_id UUID NOT NULL REFERENCES csv_previews(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    payload TEXT,
    error VARCHAR(1000),
    result TEXT,
    PRIMARY KEY (preview_id, row_number)
);
