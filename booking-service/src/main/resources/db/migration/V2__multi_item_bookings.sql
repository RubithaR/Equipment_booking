-- Phase 6: bookings become umbrella requests; per-item lifecycle moves to booking_items.
-- Drop the V1 tables and recreate from scratch — start-fresh policy.

DROP TABLE IF EXISTS booking_attachments CASCADE;
DROP TABLE IF EXISTS booking_events      CASCADE;
DROP TABLE IF EXISTS booking_items       CASCADE;
DROP TABLE IF EXISTS bookings            CASCADE;

CREATE TABLE bookings (
    id                            BIGSERIAL PRIMARY KEY,
    student_user_id               BIGINT       NOT NULL,
    student_department_id         BIGINT       NOT NULL,
    project_name                  VARCHAR(300) NOT NULL,
    purpose                       TEXT         NOT NULL,
    start_date                    TIMESTAMP    NOT NULL,
    return_date                   TIMESTAMP    NOT NULL,
    nominated_supervisor_user_id  BIGINT,
    state                         VARCHAR(40)  NOT NULL,
    created_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_items (
    id                            BIGSERIAL PRIMARY KEY,
    booking_id                    BIGINT       NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    item_id                       BIGINT       NOT NULL,
    lab_id                        BIGINT       NOT NULL,
    instructor_user_id            BIGINT       NOT NULL,
    assigned_supervisor_user_id   BIGINT,
    state                         VARCHAR(40)  NOT NULL,
    pickup_at                     TIMESTAMP,
    pickup_note                   TEXT,
    last_actor_user_id            BIGINT,
    created_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_events (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    booking_item_id BIGINT       REFERENCES booking_items(id) ON DELETE CASCADE,
    actor_user_id   BIGINT,
    from_state      VARCHAR(40),
    to_state        VARCHAR(40)  NOT NULL,
    note            TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_attachments (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT        NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    file_url            VARCHAR(1000) NOT NULL,
    file_name           VARCHAR(300)  NOT NULL,
    kind                VARCHAR(50)   NOT NULL DEFAULT 'OTHER',
    uploaded_by_user_id BIGINT,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_student              ON bookings(student_user_id);
CREATE INDEX idx_bookings_dept                 ON bookings(student_department_id);
CREATE INDEX idx_bookings_state                ON bookings(state);
CREATE INDEX idx_booking_items_booking         ON booking_items(booking_id);
CREATE INDEX idx_booking_items_item            ON booking_items(item_id);
CREATE INDEX idx_booking_items_lab             ON booking_items(lab_id);
CREATE INDEX idx_booking_items_instructor      ON booking_items(instructor_user_id);
CREATE INDEX idx_booking_items_supervisor      ON booking_items(assigned_supervisor_user_id);
CREATE INDEX idx_booking_items_state           ON booking_items(state);
CREATE INDEX idx_booking_events_book           ON booking_events(booking_id);
CREATE INDEX idx_booking_events_item           ON booking_events(booking_item_id);
CREATE INDEX idx_booking_attachments_book      ON booking_attachments(booking_id);
