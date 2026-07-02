# Plan — Availability v2 (two statuses, clearer wording, date-picker guard)

**Branch:** `feat/item-availability`
**Status of base feature:** ✅ working in Docker — catalogue already shows booking-driven
status and "booked until" (confirmed live: item #3 Raspberry Pi Kit = IN_USE, booked until
2026-07-17 17:02).

**Setup reminder (how it runs):**
- Frontend: local Vite dev server at `http://localhost:5173` (hot-reloads on save).
- Backend: 6 services in Docker (gateway 8080, eureka 8761, user 8081, equipment 8082,
  booking 8083, notification 8084). Database: Supabase.
- ⚠️ Backend changes need a **Docker image rebuild + container restart**; frontend changes do not.

---

## What the user asked for (3 changes)

1. **Only two statuses** in the student catalogue: **Available** and **In use**.
   - Collapse today's three buckets (Available / In process / In use) into two.
   - Any item a student has booked — whether it is still waiting for the instructor OR already
     approved — counts as **In use**. (So "submitted but not approved" also shows In use.)

2. **Clearer "booked until" wording.**
   - Today it reads: *"Booked until 7/17/2026, 5:02:00 PM — pick a later start."*
   - Change it to clearly say the item is free only **after** that date, e.g.
     **"Available after 7/17/2026, 5:02:00 PM"** (wording to confirm — see Phase 2).

3. **Stop the second student picking an overlapping date.**
   - When another student adds the same item and opens the cart, the date picker should only
     allow a start **after** the first student's end date.
   - Do this by setting the calendar's minimum date **and** double-checking before Submit that
     the chosen start is after the first student's end date.

---

## Phase-by-phase plan

### Phase 1 — Two statuses only (Available / In use)
- **Backend (booking-service):** in `BookingService.availability(...)`, treat **any** active
  hold as `IN_USE` (drop the IN_PROCESS headline). The per-window detail can stay, but the
  item's headline `status` becomes just `AVAILABLE` or `IN_USE`.
- **Frontend (`Equipment.jsx`):** show only **Available** / **In use**; remove the
  "In process" badge, label, and the "In process" filter dropdown option.
- **Needs:** backend rebuild (Docker) + frontend reload.

### Phase 2 — Reword the "booked until" line
- **Files:** `Equipment.jsx` (card hint) and `BookCart.jsx` (per-line note + warning banner).
- Replace *"Booked until <date> — pick a later start."* with a clear "free after" message.
  - **Proposed wording:** **"Available after <date>"** (clearest). User said "Booked after" —
    confirm which to use before coding.
- **Needs:** frontend reload only.

### Phase 3 — Guard the date picker in the cart
- **File:** `BookCart.jsx`.
- Compute the **latest** `bookedUntil` across all items currently in the cart (the booking uses
  one shared start/return window, so the booking can only begin after the most-recently-freed
  item).
- Set the Start date input's `min` to just after that date so earlier dates can't be chosen;
  set the Return date `min` to after the chosen start.
- Keep the existing overlap check as a safety net: if the chosen window still clashes, show the
  warning and keep **Submit disabled** (already implemented).
- **Needs:** frontend reload only.

### Phase 4 — Rebuild, run, verify
- [x] Rebuilt + restarted the **booking-service** container (`docker compose up -d --build
      booking-service`). Frontend builds clean; Vite serves the changes on reload.
- [x] **Verified via the live API**: every item now returns only `AVAILABLE` or `IN_USE`
      (an item that was submitted-but-not-approved, e.g. Jetson Nano, now reports `IN_USE`).
- Manual checks for **you** to do in the browser as a second student (e.g. Sachini) — hard
  reload first (Ctrl+Shift+R):
  - [ ] Catalogue shows only **Available** or **In use** (no "In process").
  - [ ] A booked item shows the new **"Booked after <date>"** wording.
  - [ ] In the cart, the calendar will not let you pick a start on/before the first student's
        end date (and a note says "start only after <date>").
  - [ ] If an overlapping date is somehow set, Submit stays disabled with a clear message.
  - [ ] Picking a start after the first student's end date → Submit works.

---

## ✅ Implemented (this round)
- **Phase 1** — `BookingService.availability()` returns `IN_USE` for any active hold;
  `Equipment.jsx` shows only Available / In use and the "In process" filter option is removed.
- **Phase 2** — card hint + cart notes now read **"Booked after <date>"**.
- **Phase 3** — `BookCart.jsx` sets the start-date calendar `min` to the latest booked-until
  in the cart (and return-date `min` follows the chosen start), with the existing overlap
  check + disabled Submit kept as the safety net.

## Note (not a bug, flag for decision)
Some seed bookings are `COLLECTED`/`OVERDUE` with a **past** return date, so their items show
"In use · Booked after <past date>". That follows your rule (any hold = in use) and is harmless
(a past window never blocks a future booking). If you'd rather hide past holds, we can show
`IN_USE` only when the booked-until date is still in the future.

---

## Decision (confirmed)
- **Phase 2 wording:** **"Booked after <date>"** (chosen by user).

---

## Not changing
- The server-side overlap rejection in `BookingService.create()` stays as the final guard.
- Items stay **addable** even when In use (a later date is allowed) — matches your rule that a
  second student *can* book the same item, just for a later window.
