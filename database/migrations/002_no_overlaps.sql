-- Apply once to an existing KR1 database. Transactional and non-destructive.
-- If legacy rows overlap, the whole migration rolls back; correct those rows first.
BEGIN;
CREATE EXTENSION IF NOT EXISTS btree_gist;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'visits_lane_no_overlap' AND conrelid = 'visits'::regclass) THEN
        ALTER TABLE visits ADD CONSTRAINT visits_lane_no_overlap EXCLUDE USING gist (
            lane_number WITH =,
            tsrange(visit_date + start_time, visit_date + start_time + duration_minutes * INTERVAL '1 minute', '[)') WITH &&
        ) WHERE (status <> 'CANCELLED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'visits_client_no_overlap' AND conrelid = 'visits'::regclass) THEN
        ALTER TABLE visits ADD CONSTRAINT visits_client_no_overlap EXCLUDE USING gist (
            client_id WITH =,
            tsrange(visit_date + start_time, visit_date + start_time + duration_minutes * INTERVAL '1 minute', '[)') WITH &&
        ) WHERE (status <> 'CANCELLED');
    END IF;
END $$;
COMMIT;
