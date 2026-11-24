CREATE TABLE lanes (
    number INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_lane_number CHECK (number > 0),
    CONSTRAINT chk_lane_name CHECK (length(trim(name)) > 0)
);

INSERT INTO lanes (number, name)
SELECT number, 'Дорожка ' || number
FROM generate_series(1, 8) AS numbers(number);

ALTER TABLE visits
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN visit_number VARCHAR(64),
    ADD COLUMN title VARCHAR(120) NOT NULL DEFAULT 'Посещение бассейна',
    ADD COLUMN description VARCHAR(1000) NOT NULL DEFAULT '',
    ADD COLUMN individual BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_visit_lane FOREIGN KEY (lane_number) REFERENCES lanes(number),
    ADD CONSTRAINT uq_visit_public_id UNIQUE (public_id),
    ADD CONSTRAINT chk_visit_title CHECK (length(trim(title)) > 0);

UPDATE visits
SET visit_number = 'VIS-' || id;

ALTER TABLE visits
    ALTER COLUMN visit_number SET NOT NULL,
    ADD CONSTRAINT uq_visit_number UNIQUE (visit_number),
    ADD CONSTRAINT chk_visit_number CHECK (length(trim(visit_number)) > 0);

CREATE FUNCTION assign_visit_number() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.visit_number IS NULL THEN
        NEW.visit_number := 'VIS-' || NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER assign_visit_number
    BEFORE INSERT ON visits
    FOR EACH ROW EXECUTE FUNCTION assign_visit_number();

CREATE FUNCTION update_visit_revision() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.version = OLD.version THEN
        NEW.version := OLD.version + 1;
    END IF;
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER update_visit_revision
    BEFORE UPDATE ON visits
    FOR EACH ROW EXECUTE FUNCTION update_visit_revision();

CREATE INDEX idx_visits_archive_date ON visits (archived, visit_date, start_time);
CREATE INDEX idx_visits_lane_number ON visits (lane_number);
