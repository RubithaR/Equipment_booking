-- Phase 3 baseline schema for booking-service.
-- Drop the old `bookings` table — schema is incompatible with the new flow.

DROP TABLE IF EXISTS booking_attachments CASCADE;
DROP TABLE IF EXISTS booking_events      CASCADE;
DROP TABLE IF EXISTS bookings            CASCADE;

CREATE TABLE bookings (
    id                            BIGSERIAL PRIMARY KEY,
    student_user_id               BIGINT       NOT NULL,
    student_department_id         BIGINT       NOT NULL,
    item_id                       BIGINT       NOT NULL,
    lab_id                        BIGINT       NOT NULL,
    instructor_user_id            BIGINT       NOT NULL,
    project_name                  VARCHAR(300) NOT NULL,
    purpose                       TEXT         NOT NULL,
    start_date                    TIMESTAMP    NOT NULL,
    return_date                   TIMESTAMP    NOT NULL,
    nominated_supervisor_user_id  BIGINT,
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

CREATE INDEX idx_bookings_student          ON bookings(student_user_id);
CREATE INDEX idx_bookings_instructor       ON bookings(instructor_user_id);
CREATE INDEX idx_bookings_item             ON bookings(item_id);
CREATE INDEX idx_bookings_lab              ON bookings(lab_id);
CREATE INDEX idx_bookings_state            ON bookings(state);
CREATE INDEX idx_booking_events_book       ON booking_events(booking_id);
CREATE INDEX idx_booking_attachments_book  ON booking_attachments(booking_id);
