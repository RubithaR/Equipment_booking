-- Multiple proposed lab-use times per LAB_ONLY line, each with a confirmed flag.
-- Stored as a JSON array of {at, confirmed} on the line (see UseSlotListConverter).
-- The single requested_use_time column stays for legacy rows / back-compat.

ALTER TABLE booking_items
    ADD COLUMN IF NOT EXISTS use_slots TEXT;
