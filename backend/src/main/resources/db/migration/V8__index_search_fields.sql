CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_clients_first_name_search ON clients USING gin (lower(first_name) gin_trgm_ops);
CREATE INDEX idx_clients_last_name_search ON clients USING gin (lower(last_name) gin_trgm_ops);
CREATE INDEX idx_clients_phone_search ON clients USING gin (phone gin_trgm_ops);
CREATE INDEX idx_visits_title_search ON visits USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_visits_number_search ON visits USING gin (lower(visit_number) gin_trgm_ops);
CREATE INDEX idx_tags_tag ON visit_tags(tag, visit_id);
CREATE INDEX idx_audit_description_search ON audit_events USING gin (lower(description) gin_trgm_ops);
CREATE INDEX idx_audit_type_search ON audit_events USING gin (lower(event_type) gin_trgm_ops);

ALTER TABLE lanes
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD CONSTRAINT uq_lane_public_id UNIQUE (public_id);
