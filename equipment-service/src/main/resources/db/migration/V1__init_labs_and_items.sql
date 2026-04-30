-- Phase 2 baseline schema for equipment-service.
-- Project intentionally starts fresh — drop the old `equipment` table.

DROP TABLE IF EXISTS items     CASCADE;
DROP TABLE IF EXISTS labs      CASCADE;
DROP TABLE IF EXISTS equipment CASCADE;

CREATE TABLE labs (
    id                  BIGSERIAL PRIMARY KEY,
    department_id       BIGINT       NOT NULL,
    name                VARCHAR(200) NOT NULL,
    location            VARCHAR(200),
    description         TEXT,
    instructor_user_id  BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (department_id, name)
);

CREATE TABLE items (
    id              BIGSERIAL PRIMARY KEY,
    lab_id          BIGINT       NOT NULL REFERENCES labs(id) ON DELETE RESTRICT,
    model           VARCHAR(200) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    serial_number   VARCHAR(100) UNIQUE,
    status          VARCHAR(40)  NOT NULL,
    description     TEXT,
    condition_note  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_labs_department ON labs(department_id);
CREATE INDEX idx_labs_instructor ON labs(instructor_user_id);
CREATE INDEX idx_items_lab       ON items(lab_id);
CREATE INDEX idx_items_status    ON items(status);
CREATE INDEX idx_items_model     ON items(model);
CREATE INDEX idx_items_category  ON items(category);
