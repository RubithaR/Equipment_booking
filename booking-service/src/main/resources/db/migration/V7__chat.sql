-- Direct chat between a student and the instructor who handled their request.
--
-- A conversation opens automatically the moment an instructor approves a booking
-- line (the line reaches READY_FOR_COLLECTION) — the same point the student is
-- notified. Both parties can then message each other; everything is stored here
-- permanently. A conversation is unique per (booking, instructor) pair, so all of
-- one instructor's chat about one booking lands in a single thread.

CREATE TABLE chat_conversations (
    id                       BIGSERIAL PRIMARY KEY,
    booking_id               BIGINT     NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    student_user_id          BIGINT     NOT NULL,
    instructor_user_id       BIGINT     NOT NULL,
    created_at               TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at          TIMESTAMP,
    student_last_read_at     TIMESTAMP,
    instructor_last_read_at  TIMESTAMP,
    CONSTRAINT uq_chat_booking_instructor UNIQUE (booking_id, instructor_user_id)
);

CREATE TABLE chat_messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT     NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_user_id   BIGINT     NOT NULL,
    body             TEXT       NOT NULL,
    created_at       TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_conv_student    ON chat_conversations(student_user_id);
CREATE INDEX idx_chat_conv_instructor ON chat_conversations(instructor_user_id);
CREATE INDEX idx_chat_messages_conv   ON chat_messages(conversation_id);
