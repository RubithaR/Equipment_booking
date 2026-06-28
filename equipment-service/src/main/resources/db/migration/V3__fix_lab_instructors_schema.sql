-- Fix lab_instructors table schema (V2 migration failed due to checksum mismatch)
-- This migration ensures the table exists with the correct instructor_id column

DROP TABLE IF EXISTS lab_instructors CASCADE;

CREATE TABLE lab_instructors (
    id              BIGSERIAL PRIMARY KEY,
    lab_id          BIGINT NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    instructor_id   BIGINT NOT NULL,
    CONSTRAINT uk_lab_instructor UNIQUE (lab_id, instructor_id)
);

CREATE INDEX idx_lab_instructors_lab ON lab_instructors(lab_id);
CREATE INDEX idx_lab_instructors_instructor ON lab_instructors(instructor_id);
