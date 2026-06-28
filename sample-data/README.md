# Sample data

Seeds the four service schemas with realistic dev data so you can sign in,
browse the catalogue, see notifications, and exercise every booking state
without clicking through the UI to create them.

## What gets seeded

| Schema          | Rows                                                     |
| --------------- | -------------------------------------------------------- |
| `users`         | 14 users — 2 HoDs, 2 Lecturers, 4 Instructors, 6 Students |
| `equipment`     | 4 labs, 12 items (mix of `AVAILABLE` / `IN_USE` / `MAINTENANCE` / `OUT_OF_SERVICE`) |
| `bookings`      | 7 bookings, one in each lifecycle state, with full event history |
| `notifications` | 7 notifications matched to those bookings                |

What this **does not** touch:

- Faculties, departments — owned by Flyway migrations
- Main Admin and the four Department Admins — owned by `SystemBootstrap` (driven by `.env`)
- Any non-`SAMPLE`-tagged rows already in the DB

## Prerequisite

Boot all four backend services at least once so:

1. Flyway provisions the schemas (`users`, `equipment`, `bookings`, `notifications`)
2. `SystemBootstrap` creates the Main Admin + Department Admins (set the `ADMIN_*` and `DEPT_ADMIN_*_EMAIL/PASSWORD` env vars in `.env`)

```sh
docker compose up --build
```

Once the four services are healthy, seed the data.

## How to seed

### Option A — Supabase SQL Editor (recommended)

1. Open your Supabase project → **SQL Editor** → **New query**.
2. Paste the contents of [`seed.sql`](seed.sql).
3. **Run**.

### Option B — `psql` from the terminal

```sh
psql "postgres://postgres.<project-ref>:<password>@aws-1-ap-south-1.pooler.supabase.com:6543/postgres" \
     -f sample-data/seed.sql
```

(Use the **session** port `5432` here, not the transaction port — `psql` runs the whole file as a single session and the `DO $$ ... $$` block needs session features.)

## Sample logins

All sample passwords are **`Password1`**.

| Role        | Email                              | Notes                          |
| ----------- | ---------------------------------- | ------------------------------ |
| HoD-CE      | `hod.computer@foe.sjp.ac.lk`       | Approves cross-dept bookings   |
| HoD-EE      | `hod.electrical@foe.sjp.ac.lk`     | Has a pending sign-off (booking #3) |
| Lecturer-CE | `lec.computer@foe.sjp.ac.lk`       |                                |
| Lecturer-ME | `lec.mechanical@foe.sjp.ac.lk`     |                                |
| Instructor  | `inst.computer@foe.sjp.ac.lk`      | Owns Sample Computer Lab A     |
| Instructor  | `inst.electrical@foe.sjp.ac.lk`    | Owns Sample Electronics Lab B  |
| Instructor  | `inst.mechanical@foe.sjp.ac.lk`    | Owns Sample Mechanical Workshop|
| Instructor  | `inst.civil@foe.sjp.ac.lk`         | Owns Sample Civil Materials Lab|
| Student     | `student1@stu.sjp.ac.lk`           | Has a `SUBMITTED` and a `REJECTED` booking |
| Student     | `student3@stu.sjp.ac.lk`           | Has an `AWAITING_SUPERVISOR` booking |
| Student     | `student4@stu.sjp.ac.lk`           | Has a `READY_FOR_COLLECTION` booking |
| Student     | `student5@stu.sjp.ac.lk`           | Has a `COLLECTED` booking      |

(Students 2, 5, and 6 also exist — see `seed.sql`.)

## Re-running

The script is **idempotent** — every run starts with `DELETE` statements that
clear the previous sample rows (matched by `SAMPLE-` serial prefixes, `SAMPLE %`
project name and lab name patterns, and the explicit list of seed user emails).
Re-run any time you want to reset the sample state.

## Wiping the sample data

To remove everything this script created without inserting new rows, run only
the **section 0 (`Wipe previous sample rows`)** at the top of `seed.sql`.
