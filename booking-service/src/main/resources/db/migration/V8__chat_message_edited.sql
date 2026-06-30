-- Allow a sender to edit their own chat message. edited_at is null until the
-- message has been edited at least once; the UI shows an "edited" marker when set.
-- (Deletes are hard deletes — the row is removed — so they need no column.)

ALTER TABLE chat_messages ADD COLUMN edited_at TIMESTAMP;
