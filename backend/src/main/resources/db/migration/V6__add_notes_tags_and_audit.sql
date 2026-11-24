CREATE TABLE visit_tags (
    visit_id BIGINT NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (visit_id, tag),
    CONSTRAINT chk_tag CHECK (length(trim(tag)) BETWEEN 1 AND 50)
);

CREATE TABLE visit_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_id BIGINT NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES web_users(id),
    content VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_note_content CHECK (length(trim(content)) > 0)
);

CREATE INDEX idx_notes_visit ON visit_notes(visit_id, created_at);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES web_users(id),
    event_type VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(1000) NOT NULL,
    correlation_id VARCHAR(36)
);

CREATE INDEX idx_audit_time ON audit_events(occurred_at DESC, id);
CREATE INDEX idx_audit_user ON audit_events(user_id);
CREATE INDEX idx_audit_entity ON audit_events(entity_id);
