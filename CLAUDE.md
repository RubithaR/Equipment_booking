# Project guidance

Spring Boot microservices equipment-booking system: 6 backend services (eureka-server, api-gateway, user-service, equipment-service, booking-service, notification-service) registering to Eureka, calling each other via Feign, sharing one Supabase Postgres database (one schema per service: `users`, `equipment`, `bookings`, `notifications`). Frontend is Vite + React. JWT auth lives only in user-service today.

Two run paths coexist:
- [docker-compose.yml](docker-compose.yml) — six containers, per-service Dockerfiles, for local dev.
- Root [Dockerfile](Dockerfile) — single container running all services, for single-image deploys.

## Use the architecture skill strictly for code improvement

The [improve-codebase-architecture](.claude/skills/improve-codebase-architecture/SKILL.md) skill is for surfacing **deepening opportunities** — turning shallow modules into deep ones for testability and AI-navigability. Use it **only** when the user's intent is architectural improvement.

**Run the skill when** the user asks things like: "where can I improve the architecture?", "what should I refactor?", "find tight coupling / shallow modules / leaky seams", "make X more testable", "review the design of Y."

**Do not run the skill for**:
- Bug fixes, dependency upgrades.
- Build, Docker, environment, or deployment issues.
- Reading, summarizing, or explaining code with no improvement intent.
- Tasks where the user has already chosen a direction and just needs implementation — at that point exit the skill's framing and switch to normal coding mode.

## Compulsory trigger: after a new feature lands

When you finish implementing a **new feature**, run the skill automatically — do not wait to be asked. A "new feature" means new behaviour: a new endpoint, page, entity, capability, or user-visible flow. It is **not** a bug fix, a rename, a config tweak, a test-only change, or a dependency bump.

Scope the post-feature run to step 1 (Explore) and step 2 (present candidates) only, focused on the **area just touched** rather than a full repo sweep. Do not push into the grilling loop unless the user picks a candidate. Acceptable to surface zero candidates if the feature already landed deep — say so explicitly.

This is a load-bearing instruction; treat it as a checklist item that follows the feature commit, not a suggestion.

## When the skill is active, follow it strictly

- Use the [LANGUAGE.md](.claude/skills/improve-codebase-architecture/LANGUAGE.md) vocabulary exactly: **module, interface, implementation, depth, seam, adapter, leverage, locality**. Do not drift to "component," "service," "API," "boundary," or "layer."
- Apply the **deletion test** before claiming a module is shallow.
- In the *candidates* step, do not propose interfaces — wait for the user to pick one.
- Create `CONTEXT.md` and `docs/adr/` lazily, only when the grilling loop calls for it.
- Surface ADR conflicts only when the friction is real enough to revisit the ADR.

## Outside the skill

Don't apply the skill's framing (depth/seams/locality) to unrelated tasks. Default project rules apply.
