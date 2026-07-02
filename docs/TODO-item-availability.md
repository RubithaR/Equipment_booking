# TODO / Plan — Booking-aware Item Availability

**Branch:** `feat/item-availability`
**Goal (from the requirement):** In the student dashboard, when a student adds equipment
to the cart and submits a booking, every other student should be able to *see* the item's
real status and should *not* be able to book the same item for the same dates.

The three rules to satisfy:

1. After a student submits a booking for an item on a specific date → the item shows **"In process"**.
2. After the instructor approves it → the item shows **"In use"**.
3. While an item is *in process* OR *in use*, another student may book the **same item** but
   **not on the same/overlapping dates** — they can only pick a date **after** the previous
   student's window. The system shows the date the item is "booked until".

---

## What was already in place (before this work)

- The backend **already rejects** a booking when the chosen dates overlap an item that is
  already held (`BookingService.create()` → `findConflicts`). So rule 3's *enforcement*
  existed — but only as a hard rejection at submit time.
- The catalogue only showed a **static** item status set by an admin (AVAILABLE / IN_USE /
  MAINTENANCE / OUT_OF_SERVICE). It was **not** driven by real bookings or dates.

**The gap:** students could not *see* "in process" / "in use" or *which dates* were taken.
That visibility is the work below.

---

## The plan

### Backend (booking-service)
- [x] Add status buckets to `BookingState`: `IN_PROCESS` (submitted / reviewing / awaiting
      supervisor) and `IN_USE` (approved / ready / collected / overdue), plus an
      `availabilityBucket(state)` helper.
- [x] New DTOs: `ActiveWindow` (one hold = itemId + state + start/end) and
      `ItemAvailabilityResponse` (itemId, status, bookedUntil, windows).
- [x] New repository query `findActiveWindows(itemIds)` — returns every active hold on a set
      of items, with its booking window.
- [x] New service method `BookingService.availability(itemIds)` — rolls the windows up to a
      headline status (IN_USE beats IN_PROCESS), the latest `bookedUntil`, and the per-window
      buckets.
- [x] New endpoint `GET /api/bookings/availability?itemIds=1,2,3` (any logged-in user).

### Frontend
- [x] `api.js` → `bookingApi.availability(itemIds)`.
- [x] **Equipment catalogue** (`Equipment.jsx`): overlay booking-driven status on each card —
      "In process" / "In use" badge + "Booked until <date> — pick a later start." Items stay
      addable (later dates allowed). Added an "In process" filter option.
- [x] **Cart** (`BookCart.jsx`): fetch availability for cart items, detect when the chosen
      start/return overlaps a held window, show an inline warning listing the clashing items
      with their booked-until date, and **disable Submit** until the dates are clear.

### Verification
- [x] Frontend `vite build` passes.
- [x] Backend `booking-service` compiles cleanly.
- [x] Confirmed the 2 failing tests are **pre-existing** (one needs the live DB; the other
      fails identically on clean `main`) — not caused by this feature.
- [ ] **Smoke test against the real DB** — start the services and call
      `GET /api/bookings/availability?itemIds=...` once. (The JPQL constructor query is only
      fully validated at runtime; it mirrors the proven `findConflicts` query, so risk is low.)
- [ ] Manual click-through: submit a booking as Student A → item shows "In process" for
      Student B; approve as instructor → shows "In use"; Student B tries the same dates →
      blocked with "booked until"; Student B picks a later date → allowed.

---

## Still to do (git / delivery)
- [ ] Decide target branch (merge `feat/item-availability` into `feat/pastupdate`, or open a PR).
- [ ] Push the branch.
- [ ] Open the pull request.

---

---

## Bug seen in demo: item still shows "Available" after a student submits

**Symptom:** Student Rubitha submitted Booking #14 for *Raspberry Pi Kit #2* (SUBMITTED, shown
in the instructor's Pending Bookings). But in another student's catalogue (Sachini, incognito)
the same item still shows green **"Available"** and an enabled *Add to cart*.

**Most likely cause (to confirm in Phase 0):** the new code is committed on branch
`feat/item-availability`, but the **running services are still the old build**. The catalogue
calls `GET /api/bookings/availability`; if the running booking-service doesn't have that
endpoint (old jar) it returns 404, and the page is written to **fall back silently** to the
old admin status — which is "Available". The frontend hot-reloads on save, but the **Java
backend does not** — it must be rebuilt and restarted.

### Phase-by-phase plan to fix

**Phase 0 — Diagnose (confirm the cause before changing anything)**
- Confirm which branch is checked out and running. The fix lives on `feat/item-availability`.
- With a student logged in, open the catalogue and watch the network tab: is the call to
  `/api/bookings/availability` returning **404** (endpoint missing) or **200 with data**?
- 404 → it's a deploy/run problem (Phase 1). 200-but-empty → it's a data/matching problem
  (Phase 3).

**Phase 1 — Run the new code**
- Make sure the branch with the feature is the one running (check out `feat/item-availability`,
  or merge it into the branch you demo from).
- **Rebuild and restart the booking-service** so the `/availability` endpoint exists.
- Restart the frontend dev server so the new catalogue/cart code is loaded.

**Phase 2 — Prove the endpoint answers**
- Call `GET /api/bookings/availability?itemIds=<RaspberryPiKit#2 id>` as a logged-in user.
- Expect: status `IN_PROCESS`, `bookedUntil = 2026-07-17 17:02`, one window for Booking #14.

**Phase 3 — Confirm the badge changes**
- Reload the other student's catalogue. *Raspberry Pi Kit #2* should now read **"In process"**
  with "Booked until 7/17/2026 — pick a later start."
- If still "Available" with a 200 response, check the item **id** matches between the catalogue
  item and the booking line (the page maps availability by `itemId`).

**Phase 4 — Confirm the date rule (rule 3)**
- As the other student, add *Raspberry Pi Kit #2*, choose dates that overlap 7/1–7/17 →
  the cart must warn and **disable Submit**.
- Choose a start **after** 7/17 → Submit is allowed.

**Phase 5 — Confirm the "in use" transition (rule 2)**
- Instructor approves Booking #14 → the item's bucket becomes `IN_USE` → catalogue badge
  changes from "In process" to **"In use"** (still booked until 7/17).

**Phase 6 — Polish & decisions (after it works)**
- The catalogue only refreshes on page load — another student must reload to see a new hold.
  Decide if that's acceptable for the demo or add a refresh.
- Confirm the wording you want: today it is **submit → "In process"**, **approve → "In use"**.
  If you'd rather show "In use" immediately on submit, that's a one-line label change — just
  decide and note it here.

---

## Notes for later (architecture review — not blocking)
The "which states hold an item" rule is now written in **three** places (two JPQL queries +
the `BookingState` sets). Worth centralising on `BookingState` so the catalogue and the
booking-create check can never disagree. (Lower priority; tracked separately.)
