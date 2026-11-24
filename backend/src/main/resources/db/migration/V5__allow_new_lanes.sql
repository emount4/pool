CREATE SEQUENCE lane_number_seq AS INT START WITH 9;

ALTER TABLE visits
    DROP CONSTRAINT chk_lane,
    ADD CONSTRAINT chk_lane CHECK (lane_number > 0);
