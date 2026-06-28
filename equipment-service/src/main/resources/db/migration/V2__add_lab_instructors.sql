-- Add lab_instructors table (missing from V1)

CREATE TABLE lab_instructors (
    id          BIGSERIAL PRIMARY KEY,
    lab_id      BIGINT NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (lab_id, user_id)
);

CREATE INDEX idx_lab_instructors_lab ON lab_instructors(lab_id);
CREATE INDEX idx_lab_instructors_user ON lab_instructors(user_id);
