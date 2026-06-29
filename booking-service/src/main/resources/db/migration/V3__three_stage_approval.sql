-- Phase 7: three-stage approval chain (HoD -> Lecturer/Supervisor -> Instructor).
-- booking_items already carries instructor_user_id (stage 3) and
-- assigned_supervisor_user_id (now the department Lecturer, stage 2).
-- Add the department HoD assignee (stage 1).

ALTER TABLE booking_items
    ADD COLUMN IF NOT EXISTS assigned_hod_user_id BIGINT;
