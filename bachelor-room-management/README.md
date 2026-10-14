# Bachelor Room Management

A web app for roommates to coordinate money, chores and communication.

- **Backend:** Java 21+ (tested design for JDK 25), Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, PostgreSQL
- **Frontend:** React 18, Vite, Tailwind CSS

## What it does

| Area | Admin of a room | Every member |
|---|---|---|
| **Rooms** | Create a room, share an invite link or code, add people by email, remove people, reset the invite link | Join by link or code, see the roommate roster, leave the room |
| **Expenses** | Log shared expenses (rent, groceries, electricity), split equally between chosen people or by custom amounts, delete expenses | See the ledger, their own share, and everyone's balance |
| **Payments** | Confirm or reject payments made to them | Log a payment to the admin, cancel it until it's confirmed |
| **Tasks** | Assign one-off tasks, create weekly chores that rotate between roommates | See their tasks, tick them off |
| **Board** | Delete any post | Post text, photos and videos (for example a broken appliance or a grocery receipt) |

Things worth knowing:

- **Admin rights are per room.** The person who creates a room is its admin. In another room they join, they're a normal member.
- **Money is exact.** Splits always add up to the cent (leftover cents go to the first people in join order), and amounts with more than 2 decimals are rejected.
- **Balance = paid − share + payments sent − payments received.** A payment only counts once it is confirmed. Positive means the room owes you, negative means you owe the room.
- **Leaving or being removed** is blocked while the person has an unsettled balance or a payment waiting for confirmation.
- **Recurring chores** become real tasks at midnight (in `APP_TIMEZONE`) on the chore's weekday. The job is safe to run twice, runs once at startup to catch up after a restart, and a failing chore never blocks the others.
- **Uploads** accept JPG, PNG, GIF, WEBP, MP4, WEBM and MOV up to 25 MB, are stored under random names, and are only served to members of that room.

## Project layout

```
backend/                 Spring Boot REST API
  src/main/java/com/brm/app/
    controller/          REST endpoints
    service/             business rules and permissions
    entity/ repository/  JPA model and queries
    util/                money splitting and chore rotation (pure logic)
    security/ config/    JWT, CORS, scheduling
  src/test/              unit tests and an end-to-end flat-share test (H2)
frontend/                React app (pages, components, API clients)
docker-compose.yml       Local PostgreSQL
.env.example             Every setting the backend understands
```

## Run it locally

You need a JDK (21 or newer; **JDK 25 works**), Maven 3.9+, Node 20+ and Docker (or your own PostgreSQL).

The backend compiles to Java 21 bytecode, which runs on any JDK from 21 to 25. It uses Spring Boot 3.5.7 and Lombok 1.18.42, the versions that work with a JDK 25 compiler. The `pom.xml` also lists Lombok as an explicit annotation processor, because JDK 23 and newer no longer run processors found on the classpath by default.

```bash
# 1. Database
docker compose up -d

# 2. Backend  (http://localhost:8080)
cd backend
mvn spring-boot:run

# 3. Frontend  (http://localhost:5173), in a second terminal
cd frontend
npm install
npm run dev
```

**No Docker or PostgreSQL?** Skip step 1 and start the backend with the built-in file database instead:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

(In PowerShell use `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"`.) Your data is saved in `backend/data/` and survives restarts.

Open http://localhost:5173, create an account, then create a room. Register a second account in a private window and join with the invite link to see the member side.

The database tables are created automatically on first start.

## Troubleshooting

**`Unable to determine Dialect without JDBC metadata` when the backend starts.** The app can't reach PostgreSQL. That message is only the last in a chain; scroll up in the log for the real cause, which is usually one of these:

| Log line above it | Meaning | Fix |
|---|---|---|
| `Connection to localhost:5432 refused` | PostgreSQL isn't running | Start it with `docker compose up -d` (Docker Desktop must be open), or start your local PostgreSQL service |
| `password authentication failed for user` | PostgreSQL is running but with different credentials | Set `DB_USERNAME` and `DB_PASSWORD` to your real ones, or create the expected user (below) |
| `database "brm_db" does not exist` | The database hasn't been created | Create it (below) |

**Using your own PostgreSQL (recommended on Windows).** Copy `backend/src/main/resources/application-local.example.yml` to `application-local.yml` in the same folder and fill in your database name, user and password. It loads automatically and is gitignored. The database must already exist (create it in pgAdmin or `CREATE DATABASE "Bachelor_room_management";`); the app creates the tables, not the database. Database names are case-sensitive when quoted, so match pgAdmin exactly.

Otherwise, to create the default user and database once, in `psql`:

```sql
CREATE USER brm_user WITH PASSWORD 'brm_password';
CREATE DATABASE brm_db OWNER brm_user;
```

Or skip PostgreSQL entirely with the `h2` profile described above.

## Run the tests

```bash
cd backend
mvn test
```

This runs the unit tests and `BrmFlowIntegrationTest`, which walks through a whole flat-share on an in-memory database: sign up, create and join a room, split rent, settle up, assign tasks, rotate a weekly chore, post a photo, and remove a roommate.

## Before you deploy

1. **Set `JWT_SECRET`.** The built-in default is public. Generate one with `openssl rand -base64 48`. The app logs a warning at startup while the default is in use.
2. Set `CORS_ORIGINS` to your real site address and `APP_TIMEZONE` to your time zone.
3. Use a real database password, and consider switching `ddl-auto` from `update` to `validate` with migrations (`backend/src/main/resources/schema-reference.sql` describes the tables).
4. Uploads are saved on the server disk (`UPLOAD_DIR`). Keep that folder on persistent storage, or swap `FileStorageService` for S3 or Firebase. Nothing else touches the filesystem.
5. Serve the frontend (`npm run build`, then the `dist/` folder) behind HTTPS and route `/api` to the backend.

## Settings

All backend settings and their defaults are listed in `.env.example`. On the frontend, `VITE_CURRENCY` (for example `INR` or `USD`) controls how money is displayed.

## API overview

All endpoints except `/api/auth/**` need `Authorization: Bearer <token>`.

```
POST   /api/auth/register | /api/auth/login        GET /api/users/me

GET    /api/rooms                                  POST /api/rooms
POST   /api/rooms/join                             GET  /api/rooms/{id}
POST   /api/rooms/{id}/invite/regenerate           POST /api/rooms/{id}/leave
GET|POST /api/rooms/{id}/members                   DELETE /api/rooms/{id}/members/{userId}

GET|POST /api/rooms/{id}/expenses                  DELETE /api/rooms/{id}/expenses/{expenseId}
GET    /api/rooms/{id}/balances

GET|POST /api/rooms/{id}/payments
POST   /api/rooms/{id}/payments/{pid}/confirm | reject      DELETE /api/rooms/{id}/payments/{pid}

GET|POST /api/rooms/{id}/tasks   (GET ?mine=true)
PUT|DELETE /api/rooms/{id}/tasks/{tid}             PATCH /api/rooms/{id}/tasks/{tid}/status
GET|POST /api/rooms/{id}/chores                    PATCH|DELETE /api/rooms/{id}/chores/{cid}

GET|POST /api/rooms/{id}/media   (multipart: caption, file)
GET    /api/rooms/{id}/media/files/{name}          DELETE /api/rooms/{id}/media/{postId}
```

## Status

All features above are implemented. At the time this was packaged, the build environment had no access to Maven Central or npm, so the project has **not yet been compiled or run end to end**. It was checked by running the money and rotation logic on its own (35 checks), by parsing the frontend, and by an independent read-through of the backend. Your first `mvn test` and `npm run dev` are the real test. If anything fails, the error message will point at the file to fix.
