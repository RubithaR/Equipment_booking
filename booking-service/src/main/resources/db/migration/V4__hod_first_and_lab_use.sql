-- HoD-first approval + lab-only equipment support.
--
-- The flow is now: student submits -> department HoD approves (stage 1, uses the
-- assigned_hod_user_id added in V3) -> lab instructor approves (stage 2). The old
-- instructor-initiated supervisor delegation is gone, so assigned_supervisor_user_id
-- is left in place but no longer written.
--
-- Each line snapshots the item's usage_type at submission so the state machine can
-- branch (BORROWABLE -> collect/return, LAB_ONLY -> instructor confirms a time).
-- requested_use_time is the lab session time the student asked for (LAB_ONLY only).

ALTER TABLE booking_items
    ADD COLUMN IF NOT EXISTS usage_type        VARCHAR(20) NOT NULL DEFAULT 'BORROWABLE';

ALTER TABLE booking_items
    ADD COLUMN IF NOT EXISTS requested_use_time TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_booking_items_hod ON booking_items(assigned_hod_user_id);
