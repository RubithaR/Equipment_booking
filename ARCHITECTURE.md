# SmartLab Equipment Booking System — Architecture

Spring Boot microservices, one API gateway, one Eureka registry, four domain services
(one of them the "smart" notification service), two shared libraries, and a single shared
Postgres database (one schema per service). Frontend is a Vite + React SPA.

---

## 1. Master diagram — request path (front door → data)

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  BROWSER   —   Vite 8 + React 19 SPA (axios, react-router)                             │
│  • JWT kept in localStorage (sl_token) → sent as  Authorization: Bearer <JWT>          │
│  • ONE axios baseURL = VITE_API_BASE (prod) | http://localhost:8080 (dev)              │
│  • No WebSocket/SSE. Realtime = setInterval polling (chat 3s/10s, staff role 15s)      │
└───────────────────────────────────────┬────────────────────────────────────────────── ┘
                                         │  HTTP(S),  path = /api/**   (cross-origin → CORS)
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  API GATEWAY   :8080      Spring Cloud Gateway (WebFlux / Netty)                        │
│  • CORS filter (allow all origin-patterns, credentials, exposes Authorization)         │
│  • DedupeResponseHeader for CORS headers                                                │
│  • Path predicate → uri  lb://<service>   (client-side load-balance over Eureka)        │
│  • ⚠ NO JWT validation here — token is forwarded downstream UNVERIFIED                  │
│  • No StripPrefix/rewrite — /api/... reaches services unchanged                         │
└───┬────────────────────────┬────────────────────────┬───────────────────────┬─────────┘
    │ /api/users             │ /api/items             │ /api/bookings         │ /api/notifications
    │ /api/faculties         │ /api/labs              │ /api/chats            │
    │ /api/departments       │                        │                       │
    ▼                        ▼                        ▼                       ▼
┌──────────────┐      ┌────────────────┐      ┌─────────────────┐      ┌─────────────────────┐
│ USER-SERVICE │      │ EQUIPMENT-SVC  │      │ BOOKING-SERVICE │      │ NOTIFICATION-SERVICE│
│   :8081      │      │    :8082       │      │     :8083       │      │       :8084         │
│              │      │                │      │                 │      │  "smart notify"     │
│ • JWT ISSUER │      │ • labs + items │      │ • bookings +    │      │ • in-app notif rows │
│ • login/reg  │      │   catalogue    │      │   item lines    │      │ • MessageRenderer   │
│ • users/org  │      │ • availability │      │ • state machine │      │   template registry │
│ • faculties  │      │   status flip  │      │ • overdue @5min │      │   (~19 event types) │
│ • depts/HoD  │      │                │      │ • 2-party chat  │      │ • polled by SPA     │
└──────┬───────┘      └───────┬────────┘      └────────┬────────┘      └──────────┬──────────┘
       │ JDBC                 │ JDBC                    │ JDBC                     │ JDBC
       │ currentSchema=users  │ ...=equipment           │ ...=bookings             │ ...=notifications
       ▼                      ▼                         ▼                          ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  POSTGRES  :5432   —   ONE physical database, ONE schema per service (HikariCP max 2)  │
│  ┌───────────┐   ┌───────────────┐   ┌────────────────────────┐   ┌───────────────┐    │
│  │ users     │   │ equipment     │   │ bookings               │   │ notifications │    │
│  │ faculties │   │ labs          │   │ bookings, booking_items│   │ notifications │    │
│  │ depts     │   │ items         │   │ booking_events         │   └───────────────┘    │
│  │ users     │   │ lab_instructors│  │ booking_attachments    │   (Hibernate ddl=update)│
│  └───────────┘   └───────────────┘   │ chat_conversations     │                         │
│  (Flyway)        (Flyway)             │ chat_messages          │                         │
│                                       └────────────────────────┘  (Flyway)               │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Service discovery — Eureka (the registry off to the side)

Every process except Postgres registers itself with Eureka and pulls the registry down so it
can resolve peers by *name* (`lb://user-service`, `@FeignClient(name="user-service")`).

```
                          ┌──────────────────────────────────┐
                          │      EUREKA SERVER   :8761         │
                          │   @EnableEurekaServer              │
                          │   • in-memory registry (NO DB)     │
                          │   • does NOT self-register         │
                          │   • does NOT fetch registry        │
                          │   • dashboard at  /                │
                          └───────────────▲────────────────────┘
              register + 30s heartbeat + fetch registry (HTTP → /eureka/)
        ┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
        │              │              │              │              │              │
   api-gateway    user-service   equipment-svc   booking-svc   notification-svc
   (also FETCHES                                  (fetch so Feign clients can resolve peers)
   registry to
   drive routing)
```

`eureka.client.service-url.defaultZone = ${EUREKA_URL:http://localhost:8761/eureka/}`
is the single knob that changes per environment (localhost / compose service name / Railway
internal domain). eureka-server has `register-with-eureka=false` and `fetch-registry=false`.

---

## 3. Service-to-service calls — Feign (resolved through Eureka)

There is **no** RestTemplate/WebClient anywhere. All east-west traffic is Feign over Apache
HttpClient5, and the target is a service *name* that Eureka resolves (or a direct `*_URL` env
override for cross-account deploys). The gateway is **not** in these paths.

```
  equipment-service ──GET /api/users/{id}──────────────────────▶ user-service
                       (LabService.verifyInstructor: is this a real, ACTIVE staff member?)

  booking-service ──GET   /api/items/{id}──────────────────────▶ equipment-service
  booking-service ──PATCH /api/items/{id}/status──────────────▶ equipment-service   (IN_USE / AVAILABLE)
  booking-service ──GET   /api/labs/{id}───────────────────────▶ equipment-service   (lab + instructorUserId)
  booking-service ──GET   /api/users/{id}──────────────────────▶ user-service        (names/emails/phones)
  booking-service ┄┄GET  /api/departments/{id}/approval-chain┄▶ user-service        (WIRED but NOT called)

  user-service    ──POST  /api/notifications───────────────────▶ notification-service (INSTRUCTOR_ACCOUNT_APPROVED)
  booking-service ──POST  /api/notifications───────────────────▶ notification-service (all booking + chat events)
```

Legend: `──▶` active edge · `┄┄▶` interface exists but is never invoked in current code.

> ⚠ Deliberately-open inter-service seams (no JWT required, "until service-to-service auth
> lands"): `GET /api/items/*`, `PATCH /api/items/*/status`, `GET /api/labs/*` on
> equipment-service, and `POST /api/notifications` on notification-service.

---

## 4. Shared libraries (compile-time coupling, not runtime edges)

```
        ┌──────────────────────────────────────┐        ┌────────────────────────────────────┐
        │  smartlab-security-commons            │        │  smartlab-notification-client        │
        │  (Spring Boot auto-config JAR)        │        │  (Feign transport + wire DTO + port) │
        │  • JwtUtil (validate/parse HS256)     │        │  • NotificationClient  @FeignClient  │
        │  • JwtAuthFilter (builds UserContext) │        │    → POST /api/notifications         │
        │  • UserContext / CurrentUser          │        │  • NotificationDispatchRequest       │
        │  • Roles / ItemStatus / UsageType     │        │    {userId, eventType, payload}      │
        │  • Exception hierarchy + BaseHandler  │        │  • Notifier<E> port + InMemoryNotifier│
        └───────────────┬──────────────────────┘        └───────────────┬──────────────────────┘
     consumed by ALL four backend services              consumed by:
     (user, equipment, booking, notification)           • booking-service   (Feign caller)
                                                         • user-service      (Feign caller)
                                                         • notification-svc  (SERVER side: uses the DTO)
                                                         • equipment-service does NOT depend on it
```

---

## 5. Cross-cutting flows

### 5a. Authentication (JWT)

```
1. Browser → POST /api/users/login  ──(gateway)──▶ user-service
2. user-service verifies BCrypt, MINTS HS256 JWT (subject=email; claims role,userId,facultyId,departmentId; 24h)
3. Browser stores token in localStorage, attaches "Authorization: Bearer <JWT>" to every request
4. Gateway forwards the header WITHOUT checking it
5. EACH backend service independently validates the token via the shared JwtAuthFilter,
   builds a UserContext, and sets authority ROLE_<role>
6. Authorization = SecurityConfig path matchers + service-layer department/ownership checks
```

- One shared `JWT_SECRET` must be identical across all services (used only to *validate*; only
  user-service *issues*).
- The filter swallows bad tokens and continues **unauthenticated** (no 401 from the filter itself).

### 5b. Notifications ("smart" = in-app, template-driven, DB-persisted)

```
booking-service / user-service
        │  build a typed domain event → FeignNotifier maps to {userId, eventType, payload}
        ▼  Feign POST /api/notifications
notification-service
        │  MessageRenderer switch on eventType (~19 templates) → {title, message}
        ▼  persist row (read=false, createdAt=now) in  notifications.notifications
Postgres
        ▲  GET /api/notifications/user/{userId}[/unread]   ← browser polls / loads on demand
Browser
```

There is **no** email, WebSocket, SSE, or message broker. "Smart" = a central template registry
so senders never hard-code copy; delivery is in-app rows the SPA reads back through the gateway.

### 5c. Booking lifecycle (state machine lives in booking-service)

```
SUBMITTED → AWAITING_HANDLER → READY_FOR_COLLECTION → COLLECTED → RETURNED
     │            │                    │                  │
     │            │                    │                  └─(past due)→ OVERDUE  (OverdueScanner @5min)
     └─ HOD_REJECT/HANDLER_REJECT → REJECTED     any cancellable line → CANCELLED
```

Single polymorphic endpoint `POST /api/bookings/{id}/items/{itemId}/transition` with a
discriminated `{type: HOD_APPROVE | HANDLER_APPROVE | MARK_COLLECTED | MARK_RETURNED | ...}` body.
Each transition guards states → checks role → mutates → records a BookingEvent → optionally flips
the physical item status (Feign to equipment-service) → emits notifications.

---

## 6. Ports & responsibilities

| Process              | Port  | DB schema      | Registers w/ Eureka | Role |
|----------------------|-------|----------------|---------------------|------|
| eureka-server        | 8761  | —              | no (IS the registry)| Service registry / dashboard |
| api-gateway          | 8080  | —              | yes (+fetches)      | Single front door, routing, CORS |
| user-service         | 8081  | `users`        | yes                 | Auth/JWT issuer, users, faculties, departments |
| equipment-service    | 8082  | `equipment`    | yes                 | Labs, items, availability status |
| booking-service      | 8083  | `bookings`     | yes                 | Bookings, state machine, chat, overdue |
| notification-service | 8084  | `notifications`| yes                 | In-app notifications (template registry) |
| postgres             | 5432  | (all 4 schemas)| no                  | Shared database |

---

## 7. Communication matrix (verified)

| From | To | Protocol | Path via | Purpose |
|------|----|----------|----------|---------|
| Browser | api-gateway | HTTP + CORS, Bearer JWT | direct | All `/api/**` traffic |
| api-gateway | user-service | HTTP proxy | `lb://` (Eureka LB) | `/api/users,faculties,departments/**` |
| api-gateway | equipment-service | HTTP proxy | `lb://` (Eureka LB) | `/api/items,labs/**` |
| api-gateway | booking-service | HTTP proxy | `lb://` or `BOOKING_SERVICE_URI` | `/api/bookings,chats/**` |
| api-gateway | notification-service | HTTP proxy | `lb://` or `NOTIFICATION_SERVICE_URI` | `/api/notifications/**` |
| equipment-service | user-service | Feign/HTTP | Eureka / `USER_SERVICE_URL` | Verify lab instructor |
| booking-service | equipment-service | Feign/HTTP | Eureka / `EQUIPMENT_SERVICE_URL` | Item + lab lookup, status flip |
| booking-service | user-service | Feign/HTTP | Eureka / `USER_SERVICE_URL` | People lookup for notif/chat |
| booking-service | user-service | Feign/HTTP | Eureka | approval-chain — **wired, not called** |
| user-service | notification-service | Feign/HTTP | Eureka / `NOTIFICATION_SERVICE_URL` | INSTRUCTOR_ACCOUNT_APPROVED |
| booking-service | notification-service | Feign/HTTP | Eureka / `NOTIFICATION_SERVICE_URL` | All booking + chat events |
| each service | Eureka | Eureka client/HTTP | direct | register + heartbeat + fetch |
| each service | Postgres | JDBC (HikariCP) | direct | own schema only |

---

## 8. Two deployment paths

**A. docker-compose (local dev)** — 7 containers on one `smartlab` bridge network: `postgres`
(with healthcheck + `pgdata` volume), `eureka-server`, `user/equipment/booking/notification`,
`api-gateway`. Services address each other by compose name (`postgres:5432`,
`eureka-server:8761`). `supabase-extractor` is profile-gated off. Frontend runs separately
(`cd frontend && npm run dev`). Boot order via `depends_on`: postgres(healthy)+eureka →
4 services → gateway.

**B. Railway (cloud)** — each service its own image + private `*.railway.internal` domain; only
api-gateway gets a public domain. `combined-services/Dockerfile` optionally packs
equipment+booking+notification into ONE container (3 JVMs via `start.sh`, `-Xmx200m` each,
`wait -n` restart-all-on-any-exit) to cut Railway service count; eureka-server, api-gateway,
user-service stay separate. Env-driven wiring: `EUREKA_URL`, `DB_URL`, `JWT_SECRET`,
`EUREKA_PREFER_IP=false`, `EUREKA_INSTANCE_HOSTNAME=<svc>.railway.internal`.

**Database**: now an own Postgres container (`postgres/Dockerfile`, `postgres:17-alpine`) in both
paths — Supabase has been replaced and survives only as (1) hardcoded fallback defaults in each
service's `${DB_URL:...}`, and (2) `supabase-extractor` — a one-shot `pg_dump` that seeds
`postgres/init/02-supabase-data.sql`. `init/01-create-schemas.sql` pre-creates all four schemas
(needed because notification-service uses `ddl-auto=update` and won't create its own schema).

---

## 9. Known gaps (from the code, worth a follow-up)

- **Gateway does zero auth** — every service re-validates the JWT itself; a misconfigured service
  would be wide open since the edge trusts the header.
- **user-service SecurityConfig matches a non-existent path** (`PATCH /api/users/*/approve`), so
  `/assign-role`, `/approve-student`, `/students/pending`, `/reject-student` fall through to
  `authenticated()` and rely only on service-layer department scoping.
- **notification-service read/patch/delete have no per-recipient ownership check** — any
  authenticated user can read/modify any `userId`'s notifications.
- **Dead code**: `DepartmentClient.getApprovalChain` and `UserClient.getByRole` are defined but
  never invoked; `LabInstructor` entity/repo appear unused (labs use `instructor_user_id`).
```
