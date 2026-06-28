# Smart Lab — Equipment Booking

Spring Boot microservices backend with a Vite + React frontend for booking lab equipment. Six backend services register with Eureka and call each other via Feign; all services share one Supabase Postgres database (one schema per service: `users`, `equipment`, `bookings`, `notifications`). JWT authentication is handled by `user-service`.

## Services and ports

| Service                | Port  | Container             |
| ---------------------- | ----- | --------------------- |
| `eureka-server`        | 8761  | `smartlab-eureka`     |
| `api-gateway`          | 8080  | `smartlab-gateway`    |
| `user-service`         | 8081  | `smartlab-user`       |
| `equipment-service`    | 8082  | `smartlab-equipment`  |
| `booking-service`      | 8083  | `smartlab-booking`    |
| `notification-service` | 8084  | `smartlab-notification` |
| `frontend` (Vite)      | 5173  | run on host           |

Eureka dashboard: <http://localhost:8761>. Gateway entry point: <http://localhost:8080>.

## Prerequisites

- Docker Desktop (Compose v2)
- Node.js 18+ and npm (for the frontend)
- A Supabase project with the four schemas created and the DB password to hand

## First-time setup

```bash
cp .env.example .env
# edit .env and set DB_PASSWORD to your Supabase password
# if your Supabase project uses a different pooled URL/role, also set DB_URL and DB_USERNAME there
```

Optional overrides in `.env` (defaults are baked into each service's `application.properties`):

```bash
JWT_SECRET=...
ADMIN_EMAIL=admin@smartlab.local
ADMIN_PASSWORD=Admin123
```

`.env` is gitignored — never commit it.

## Run the backend (Docker Compose)

```bash
docker compose up --build         # build + start all six services
docker compose up                 # start without rebuilding
docker compose down               # stop and remove containers
docker compose down -v            # also remove the network/volumes
docker compose logs -f            # tail logs from every service
docker compose logs -f user-service
docker compose restart booking-service
docker compose ps                 # see what's running
```

Wait for `eureka-server` to log "Started EurekaServerApplication" before the others finish registering. The first build pulls Maven + JDK images and takes a few minutes; subsequent builds are cached.

## Run the frontend

```bash
cd frontend
npm install
npm run dev                       # http://localhost:5173
npm run build                     # production bundle
npm run lint
```

The frontend talks to the gateway at `http://localhost:8080` by default.

## Common workflows

Rebuild a single service after a code change:

```bash
docker compose up --build user-service
```

Shell into a running container:

```bash
docker exec -it smartlab-user sh
```

Stop everything and free all ports:

```bash
docker compose down
```

## Troubleshooting

**`Bind for 0.0.0.0:8761 failed: port is already allocated`** — something else is bound to a port Compose needs. Find it and stop it:

```bash
lsof -nP -iTCP:8761 -sTCP:LISTEN              # which process holds the port
docker ps --filter "publish=8761"             # is another container holding it
docker stop <container-name-or-id>
```

A frequent cause is a previously launched single-image container (built from an older root `Dockerfile`) still running with all six service ports published. Stop that container before running `docker compose up`.

**Service shows up in Eureka but Feign calls fail** — the gateway and services resolve each other by service name on the `smartlab` Docker network. If you run a service outside Compose, point it at `http://localhost:8761/eureka/` and make sure it registers with a host the others can reach.

**DB connection errors** — confirm `DB_PASSWORD` in `.env` matches the current Supabase password (Project Settings → Database). Compose only reads `.env` at `up` time; restart the stack after editing.

## Project layout

```
.
├── docker-compose.yml        # six-service local dev stack
├── .env.example              # template for DB_PASSWORD and overrides
├── eureka-server/            # service registry
├── api-gateway/              # Spring Cloud Gateway, single entry point
├── user-service/             # auth, JWT, user CRUD
├── equipment-service/        # equipment catalog
├── booking-service/          # bookings, calls equipment + user via Feign
├── notification-service/     # email/notification dispatch
└── frontend/                 # Vite + React client
```

Each backend service has its own `Dockerfile` (multi-stage Maven build → JRE runtime) and its own `application.properties` under `src/main/resources/`.
