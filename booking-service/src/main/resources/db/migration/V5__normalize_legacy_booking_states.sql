-- Normalize booking states left over from earlier flows.
--
-- The state machine has been through a few shapes (instructor-first, an
-- interim HoD-first with AWAITING_HOD/LAB_CONFIRMED, and an optional
-- supervisor-delegation stage). The current model uses a single closed set:
--   SUBMITTED, AWAITING_HANDLER, READY_FOR_COLLECTION,
--   COLLECTED, RETURNED, OVERDUE, REJECTED, CANCELLED, COMPLETED
--
-- Rows created by an older build can carry a state that no longer exists, so
-- the new queries (e.g. the HoD dashboard, which filters on SUBMITTED) skip
-- them and they look "stuck". Map every legacy state to its current
-- equivalent. Idempotent: only rows still holding a legacy state are touched.

-- Awaiting the department HoD's first review.
UPDATE bookings.booking_items SET state = 'SUBMITTED' WHERE state = 'AWAITING_HOD';
UPDATE bookings.bookings      SET state = 'SUBMITTED' WHERE state = 'AWAITING_HOD';

-- Past the HoD, now in a staff member's queue (instructor review / supervisor delegation).
UPDATE bookings.booking_items
   SET state = 'AWAITING_HANDLER'
 WHERE state IN ('INSTRUCTOR_REVIEWING', 'AWAITING_SUPERVISOR', 'SUPERVISOR_APPROVED');
UPDATE bookings.bookings
   SET state = 'AWAITING_HANDLER'
 WHERE state IN ('INSTRUCTOR_REVIEWING', 'AWAITING_SUPERVISOR', 'SUPERVISOR_APPROVED');

-- Lab-only confirmations from the interim flow are just "ready" in the current model.
UPDATE bookings.booking_items SET state = 'READY_FOR_COLLECTION' WHERE state = 'LAB_CONFIRMED';
UPDATE bookings.bookings      SET state = 'READY_FOR_COLLECTION' WHERE state = 'LAB_CONFIRMED';

-- Any flavour of rejection/decline collapses to REJECTED.
UPDATE bookings.booking_items
   SET state = 'REJECTED'
 WHERE state IN ('HOD_REJECTED', 'INSTRUCTOR_REJECTED', 'SUPERVISOR_DECLINED');
UPDATE bookings.bookings
   SET state = 'REJECTED'
 WHERE state IN ('HOD_REJECTED', 'INSTRUCTOR_REJECTED', 'SUPERVISOR_DECLINED');
