# CampusConnect – Backend

REST API for **CampusConnect**, a campus issue and complaint management system. Students register, submit complaints and track their status; administrators review, update and resolve them.

**Stack:** Java 21 · Spring Boot 4 (Web MVC, Data JPA, Validation) · Maven · MySQL · Docker (Render)

Frontend repository: `CampusConnect-Frontend` (React + Vite, deployed on Vercel).

---

## Architecture

```
React (Vercel) ──HTTP/JSON──► Spring Boot API (Render) ──JPA──► MySQL (cloud)
```

The code follows a controller → service → repository layout:

```
src/main/java/com/campusconnect
├── CampusConnectApplication.java
├── config/        AuthInterceptor (token check), WebConfig (CORS), DataInitializer (admin account)
├── controller/    AuthController, ComplaintController, AdminController, MetaController
├── dto/           Request/response records with validation rules
├── entity/        User, Complaint, StatusUpdate + enums Role, ComplaintCategory, ComplaintStatus
├── exception/     ApiException, GlobalExceptionHandler, ApiError (consistent JSON errors)
├── repository/    Spring Data JPA repositories
└── service/       AuthService, ComplaintService (business rules)
```

### Data model

| Table | Purpose |
|---|---|
| `users` | Students and administrators. Passwords are stored as BCrypt hashes. |
| `complaints` | Complaint ID, title, description, category, location, status, admin remarks, timestamps, and the student who submitted it. |
| `status_updates` | Status history for each complaint, used for the tracking timeline. |

Each complaint gets a readable reference number derived from its ID, e.g. `CC-00042`.

**Categories:** Infrastructure, Electricity, Water Supply, Cleanliness, Wi-Fi and Internet, Classroom or Laboratory Issues, Other Campus Issues
**Statuses:** Submitted → In Progress → Resolved, or Rejected (a reason is required)

### Authentication

`POST /api/auth/login` and `POST /api/auth/register` return a random 64-character token. The frontend sends it as `Authorization: Bearer <token>`. `AuthInterceptor` resolves the user for every protected `/api/**` route and requires the `ADMIN` role for `/api/admin/**`. Students can only self-register; the administrator account is created from environment variables on startup.

---

## API

| Method | Endpoint | Access | Description | Success |
|---|---|---|---|---|
| GET | `/api/health` | Public | Health check (also reports database status) | 200 |
| GET | `/api/meta` | Public | Categories and statuses for dropdowns | 200 |
| POST | `/api/auth/register` | Public | Register a student, returns `{ token, user }` | 201 |
| POST | `/api/auth/login` | Public | Log in, returns `{ token, user }` | 200 |
| GET | `/api/auth/me` | Logged in | Current user | 200 |
| POST | `/api/auth/logout` | Logged in | Revoke token | 204 |
| POST | `/api/complaints` | Student | Submit a complaint | 201 |
| GET | `/api/complaints/my` | Student | My complaints (newest first) | 200 |
| GET | `/api/complaints/{id}` | Owner / Admin | Complaint details + status history | 200 |
| PUT | `/api/complaints/{id}` | Owner | Edit while status is *Submitted* | 200 |
| DELETE | `/api/complaints/{id}` | Owner / Admin | Withdraw (owner, while *Submitted*) or delete (admin) | 204 |
| GET | `/api/admin/complaints?status=&category=&search=` | Admin | All complaints with optional filters | 200 |
| PATCH | `/api/admin/complaints/{id}/status` | Admin | Update status (+ remarks) | 200 |
| GET | `/api/admin/stats` | Admin | Counts per status | 200 |

Errors always use the same JSON shape:

```json
{
  "timestamp": "2026-10-12T09:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Please correct the highlighted fields",
  "path": "/api/complaints",
  "fieldErrors": { "title": "Title must be 5 to 150 characters" }
}
```

Status codes used: `400` validation / bad input, `401` missing or invalid token, `403` wrong role or not the owner, `404` not found, `409` duplicate email or complaint no longer editable.

Example:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@campusconnect.edu","password":"Admin@123"}'
```

---

## Configuration

All settings live in `src/main/resources/application.properties` and can be overridden with environment variables:

| Variable | Default (local) | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port (Render sets this automatically) |
| `DB_URL` | `jdbc:mysql://localhost:3306/campusconnect?createDatabaseIfNotExist=true&...` | JDBC URL |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | `root` | Database password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated frontend origins. Wildcards are allowed, e.g. `https://campusconnect-*.vercel.app` |
| `ADMIN_NAME` | `Campus Administrator` | Display name of the admin account |
| `ADMIN_EMAIL` | `admin@campusconnect.edu` | Admin login email |
| `ADMIN_PASSWORD` | `Admin@123` | Admin password. **Always change this in production.** It is re-applied on every startup. |
| `DDL_AUTO` | `update` | Hibernate schema mode. Tables are created automatically. |
| `SHOW_SQL` | `false` | Log SQL statements |
| `DB_POOL_SIZE` | `5` | Max database connections (keep it small on free databases) |

Production credentials are never committed. They are set as environment variables on Render.

---

## Run locally

Prerequisites: JDK 21 and a MySQL 8 server (the database `campusconnect` is created automatically).

```bash
# Windows PowerShell: set your local MySQL password if it isn't "root"
$env:DB_PASSWORD="your_mysql_password"

./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The API runs at http://localhost:8080. Check http://localhost:8080/api/health.

### Tests

```bash
./mvnw test
```

The integration tests use an in-memory H2 database, so they do not need MySQL. They cover registration, login, validation errors, role checks, CORS, and the full complaint lifecycle.

---

## Deploy to Render

Render has no native Java runtime, so this repo includes a **`Dockerfile`**:

- **Build:** `mvn clean package -DskipTests` produces `target/campusconnect-backend.jar`
- **Start:** `java $JAVA_OPTS -jar app.jar`, tuned for the 512 MB free instance

Steps:

1. Create a MySQL database (for example Aiven's free MySQL plan) and note the host, port, database, user and password.
2. In Render, go to **New → Web Service**, connect this GitHub repo, and set **Language: Docker** and **Instance type: Free**.
3. Add these environment variables:
   - `DB_URL` = `jdbc:mysql://HOST:PORT/DATABASE?sslMode=REQUIRED`
   - `DB_USERNAME`, `DB_PASSWORD`
   - `ADMIN_EMAIL`, `ADMIN_PASSWORD`
   - `CORS_ALLOWED_ORIGINS` = your Vercel URL, e.g. `https://campusconnect.vercel.app`
4. Under **Advanced → Health Check Path**, enter `/api/health`.
5. Deploy, then open `https://<service>.onrender.com/api/health`. It should return `{"status":"UP","database":"UP",...}`.

> Render's free instances sleep after about 15 minutes without traffic. The first request after that can take up to a minute; the frontend shows a "server is waking up" message during that time.
