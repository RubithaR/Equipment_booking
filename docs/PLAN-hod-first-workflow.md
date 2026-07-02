# Plan — HOD-first booking approval workflow

**Branch (proposed):** `feat/hod-first-approval`
**Setup:** frontend local Vite (5173), 6 backend services in Docker, Supabase DB.
⚠️ Backend changes need a Docker image rebuild + restart; frontend changes only need a reload.

---

## The new workflow (confirmed with you)

```
Student submits booking
        │
        ▼
Student's DEPARTMENT HOD  ──reject──► REJECTED (student notified)
   reviews the booking
   (sees: student name, purpose, date period, lab name,
          and the lab's default instructor)
        │ approve + assign a handler (per item)
        │ handler = Instructor / Lecturer / HOD (any department)
        ▼
Assigned HANDLER  ──reject──► REJECTED (student notified)
   approves (sets pickup details)
        │
        ▼
READY FOR COLLECTION  →  COLLECTED  →  RETURNED   (overdue scan still applies)
   (student notified at each step)
```

### Confirmed decisions
1. **Reviewer = the student's department HOD** (not the lab's department).
2. **Handler = Instructor, Lecturer, or HOD** — HOD may pick staff from any department.
3. **Remove** the old "instructor forwards to a supervisor" delegate flow entirely.
4. The **whole booking** goes to the student's department HOD; the HOD assigns a handler
   **per item line** (different items can go to different handlers / departments).

---

## State machine changes

### New per-item states
| State | Meaning |
|-------|---------|
| `SUBMITTED` | Awaiting the student's department HOD (initial) |
| `AWAITING_HANDLER` | HOD approved & assigned a handler; awaiting the handler's approval (**new**) |
| `READY_FOR_COLLECTION` | Handler approved; student can collect (unchanged) |
| `COLLECTED` / `RETURNED` / `OVERDUE` | unchanged |
| `REJECTED` | Rejected by HOD or handler (replaces `INSTRUCTOR_REJECTED`) |
| `CANCELLED` | Student cancelled (unchanged) |

### Removed states (delegate flow gone)
`INSTRUCTOR_REVIEWING`, `AWAITING_SUPERVISOR`, `SUPERVISOR_APPROVED`, `SUPERVISOR_DECLINED`.

### New transitions (replace START_REVIEW / DELEGATE / SUPERVISOR_* / FINALISE)
| Transition | From → To | Who |
|-----------|-----------|-----|
| `HOD_APPROVE` (handlerUserId) | SUBMITTED → AWAITING_HANDLER | HOD of student's dept |
| `HOD_REJECT` (reason) | SUBMITTED → REJECTED | HOD of student's dept |
| `HANDLER_APPROVE` (pickupAt, pickupNote) | AWAITING_HANDLER → READY_FOR_COLLECTION | the assigned handler |
| `HANDLER_REJECT` (reason) | AWAITING_HANDLER → REJECTED | the assigned handler |
| `MARK_COLLECTED` / `MARK_RETURNED` | unchanged targets | the assigned handler |
| `FLIP_OVERDUE` / `CANCEL` | unchanged | system / student |

### New transition Roles (in `transition/Role.java`)
- `HOD_OF_STUDENT_DEPT` — user is HOD **and** `user.departmentId == booking.studentDepartmentId`
  (uses the JWT department claim — **no extra lookup needed**).
- `HANDLER_ASSIGNED` — user is the line's assigned handler (any staff role), i.e.
  `user.id == line.instructorUserId`.

### Data model — **no DB migration needed**
- Reuse `booking_items.instructor_user_id` to hold the **assigned handler** (set by HOD, not at
  submission). At submission it holds the lab's *default* instructor as a suggestion for the HOD.
- `assigned_supervisor_user_id` becomes unused (left in place, harmless).
- New states are just new string values (column is `varchar(40)`).

---

## Phase-by-phase plan

### Phase 1 — Backend state machine (booking-service)
- `entity/BookingState.java`: add `AWAITING_HANDLER`, `REJECTED`; drop the 4 removed states from
  the sets; update `ACTIVE`, `IN_USE` (availability), `rollUp` priority, `isCancellable`.
- `transition/Role.java`: add `HOD_OF_STUDENT_DEPT`, `HANDLER_ASSIGNED`; remove `SUPERVISOR_ASSIGNED`.
- `transition/Transition.java`: add `HodApprove`, `HodReject`, `HandlerApprove`, `HandlerReject`;
  remove `StartReview`, `Delegate`, `Finalise`, `SupervisorApprove`, `SupervisorDecline`,
  rename `Reject`→ handler/HOD rejects, `ApproveDirectly`→ folded into `HandlerApprove`.
- `auth/BookingAuthorizer.java` + `TransitionEngine`: map the two new Roles to concrete checks
  (HOD-of-student-dept via JWT dept; handler via assigned id).

### Phase 2 — Routing & queries (booking-service)
- `BookingService.create()`: stop blocking when a lab has no instructor; set each line to
  `SUBMITTED` with `instructorUserId` = lab's default instructor (suggestion). Notify the
  student's department HOD + student ack (resolve the dept HOD — see open item below).
- New reads:
  - `GET /api/bookings/awaiting-hod` → bookings with a line in `SUBMITTED` where
    `studentDepartmentId == caller.departmentId` and caller is HOD.
  - Keep `GET /api/bookings/assigned-to-me` → now returns lines in `AWAITING_HANDLER` (and later
    states) assigned to the caller (handler).
- Remove `awaiting-my-supervision`.

### Phase 3 — Notifications (booking-service)
- New events: `SubmittedToHod` (to HOD), `HodApprovedToHandler` (to handler),
  `HodApprovedAckToStudent`, `HandlerApprovedReady` (reuse ReadyForCollection),
  `Rejected` (to student). Remove the supervisor-related events.

### Phase 4 — Frontend
- **HOD dashboard** (new, `pages/hod/`) with **two tabs**:
  - **Tab 1 — Student Requests** (pending): bookings awaiting my review; per line show item, lab,
    lab's default instructor; a staff picker (search Instructor/Lecturer/HOD, any department) +
    Approve (assign handler) / Reject.
  - **Tab 2 — Approved / Sent**: every request I've already approved, showing **who I sent it to**
    (the assigned handler) and the current status (awaiting handler / ready / collected / etc.).
  - Backend: `GET /api/bookings/awaiting-hod` (Tab 1) and `GET /api/bookings/hod-processed`
    (Tab 2 — non-SUBMITTED lines in my department).
- **Handler page** (`pages/instructor/PendingBookings.jsx` → generalised): lines assigned to me in
  `AWAITING_HANDLER`; Approve (pickup details) / Reject; plus collected/returned actions.
- **Remove** the supervisor delegate UI (`pages/supervisor/Queue.jsx`) and delegate buttons.
- `auth.js` `homePathFor`: HOD → HOD review queue; Instructor/Lecturer → handler queue.
- `api.js`: new transition wrappers (`hodApprove`, `hodReject`, `handlerApprove`, `handlerReject`),
  `awaitingHod()`; drop supervisor wrappers.
- `NavBar.jsx` + `App.jsx` routes: HOD nav + route; relabel instructor nav.

### Phase 5 — Rebuild, run, verify
- Rebuild booking-service (and user-service if touched) Docker images; reload frontend.
- End-to-end test: student books → appears in HOD queue → HOD assigns a lecturer → shows in that
  lecturer's queue → lecturer approves → student sees READY_FOR_COLLECTION. Plus reject paths.

---

## Open item to resolve during Phase 2
**Notifying the HOD at submission needs the HOD's user id for the student's department.**
The queue & authorization don't need it (they use `studentDepartmentId` + the JWT dept claim), but
a push notification does. Options:
- (a) add a small `DepartmentClient` Feign call to fetch `department.hodUserId`, or
- (b) skip the push and rely on the HOD seeing their queue (simplest), or
- (c) look up the dept's HOD via the existing user search.
**Default: (a)** so the HOD gets notified, falling back to no-notification if unresolved.

---

## ✅ Status — implemented & verified
- All 6 phases done. Booking-service rebuilt in Docker and started cleanly (new JPQL parses).
- Backend unit tests rewritten and **passing** (TransitionEngine + Authorizer, 39 tests green).
- Smoke test: availability still works; `/awaiting-hod` exists & is HOD-guarded; old
  `/awaiting-my-supervision` removed. Frontend builds clean.
- **Decisions taken during build:** reviewer = student's department HOD (JWT dept match, no
  lookup); handler stored in `instructor_user_id` (no migration); notifications reuse existing
  events (handler assignment reuses the old "delegate" event — eventType label is cosmetic).
- **Left for you:** UI click-through (student → HOD assigns → handler approves → collect), as a
  second-by-second demo check.

## Risk / size note
This reorders a core domain and touches ~15–20 files across booking-service and the frontend.
It is the biggest change so far. Existing in-flight bookings in old states (e.g.
`AWAITING_SUPERVISOR`) would no longer have valid transitions — for a demo DB that's fine, but
worth knowing. Tests in `booking-service/src/test` (TransitionEngineTest, BookingServiceCreateTest)
will need updating to the new states.
