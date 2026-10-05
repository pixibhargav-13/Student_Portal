# Project Guide

How the Student Portal backend is organised, how to run, build and test it, and how to add new
features. Git workflow and naming rules are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Contents

1. [Tech stack](#1-tech-stack)
2. [Project structure](#2-project-structure)
3. [Prerequisites](#3-prerequisites)
4. [First-time setup](#4-first-time-setup)
5. [Run](#5-run)
6. [Build and test](#6-build-and-test)
7. [Configuration](#7-configuration)
8. [Database](#8-database)
9. [How a request flows](#9-how-a-request-flows)
10. [Adding a new module](#10-adding-a-new-module)
11. [Auth and error codes (for frontend)](#11-auth-and-error-codes-for-frontend)
12. [API list](#12-api-list)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Tech stack

Same versions as Parmeshwari ERP. Do not upgrade one library alone; versions are pinned in the root `pom.xml`.

| What | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.2.0 (Spring Framework 6.2.0) |
| Database | PostgreSQL 17 |
| API docs | OpenAPI 3 YAML + springdoc 2.8.2 (Swagger UI) |
| Code generation | openapi-generator-maven-plugin 7.4.0 |
| Mapping | MapStruct 1.5.5.Final |
| Boilerplate | Lombok 1.18.40 |
| Auth | Own JWT (jjwt 0.12.3) + mobile OTP. No Cognito. |
| Formatting | fmt-maven-plugin 2.21 (Google Java Format, runs on every build) |
| Build | Maven wrapper (`mvnw` / `mvnw.cmd`, Maven 3.9.11) |

## 2. Project structure

Multi-module Maven project. Each business area is its own module; `web-app` wires them into one runnable app.

```
backend/
├── pom.xml                       # parent: versions, modules, plugins
├── api-contract/                 # API contract (source of truth for every endpoint)
│   ├── pom.xml                   # one openapi-generator <execution> per spec
│   └── src/main/
│       ├── resources/
│       │   └── user-management.yaml
│       └── java/com/studentportal/config/OpenApiConfig.java   # serves specs to Swagger
├── common/                       # shared by all modules, no business logic
│   └── src/main/java/com/studentportal/common/
│       ├── audit/AuditInfo.java          # created_at / last_updated_at on every entity
│       ├── repository/CoreRepository.java# JpaRepository + JpaSpecificationExecutor
│       ├── util/PaginationUtils.java     # page/size/sort -> PageRequest (sort whitelist)
│       └── exception/                    # ApiException + BadRequest/NotFound/Conflict/...
├── user-management/              # login, JWT security, profile, admin users
│   └── src/main/java/com/studentportal/usermanagement/
│       ├── config/       # security filter chain, CORS, @ConfigurationProperties
│       ├── controller/   # implements generated *Api interfaces, no logic
│       ├── service/      # business logic, transactions
│       ├── repository/   # Spring Data repositories
│       ├── model/entity/ # JPA entities + enums
│       ├── mapper/       # MapStruct entity <-> API model
│       ├── security/     # JWT filter, AuthUser, CurrentUser, role annotations
│       └── sms/          # SmsSender interface (logging stub for now)
└── web-app/                      # the runnable Spring Boot app
    └── src/main/
        ├── java/com/studentportal/
        │   ├── WebAppApplication.java
        │   └── exceptionhandler/GlobalExceptionHandler.java
        └── resources/
            ├── application.yaml                  # shared config, reads env values
            ├── application-local.example.yaml    # copy -> application-local.yaml
            └── schema/                           # SQL run on every startup
                ├── init.sql
                └── users.sql
```

Every feature module uses the same packages: `config`, `controller`, `service`, `repository`,
`model/entity`, `mapper`. Keep that layout so anyone can find things in any module.

**Generated code** lives in `api-contract/target/generated-sources/openapi` and is rebuilt on every
build. Never edit it; change the YAML instead.

## 3. Prerequisites

- **JDK 21** (`C:\Program Files\Java\jdk-21`). JDK 25 can run the app, but the formatter plugin fails
  on it during the build.
- **PostgreSQL 17** running on `localhost:5432`.
- **IntelliJ IDEA** with the Lombok plugin (bundled) and annotation processing enabled
  (Settings → Build → Compiler → Annotation Processors → Enable).
- Git Bash on Windows for the commands below.

You don't need to install Maven; use the wrapper.

## 4. First-time setup

1. Clone:
   ```bash
   git clone https://github.com/pixibhargav-13/Student_Portal.git
   ```
2. Create the database (psql or pgAdmin):
   ```sql
   CREATE DATABASE studentportal;
   ```
   The `studentportal` schema and all tables are created automatically on startup.
3. Create your local config:
   ```bash
   cp web-app/src/main/resources/application-local.example.yaml web-app/src/main/resources/application-local.yaml
   ```
   Fill in `DB_PASSWORD`, `BOOTSTRAP_ADMIN_PASSWORD`, and a JWT key:
   ```bash
   openssl rand -base64 32
   ```
   `application-local.yaml` is git-ignored. Never commit it.
4. Build once so the API interfaces are generated (IntelliJ shows errors until you do):
   ```bash
   JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw.cmd clean install -DskipTests
   ```

## 5. Run

**IntelliJ:** open the `backend` folder as a Maven project, set Project SDK to 21, and run
`web-app/src/main/java/com/studentportal/WebAppApplication.java`. The `local` profile is active by
default (`SPRING_PROFILES_ACTIVE` overrides it).

**Terminal:**
```bash
"/c/Program Files/Java/jdk-21/bin/java" -jar web-app/target/web-app-1.0.0-SNAPSHOT.jar
```

Then open:

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Raw spec (for frontend type generation) | http://localhost:8080/doc/v3/user-management |

On first start an admin is created from `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`, but
only if no admin exists yet. Log in with it via `POST /api/v1/auth/signin`, then click **Authorize**
in Swagger and paste the `accessToken`.

To log in as a parent or tutor locally, call `POST /api/v1/auth/otp/send`; with `OTP_DEV_MODE: true`
the OTP comes back as `devOtp`. The SMS text is also printed in the server log.

## 6. Build and test

| Task | Command |
|---|---|
| Full build + tests | `JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw.cmd clean install` |
| Build, skip tests | add `-DskipTests` |
| Tests of one module | `./mvnw.cmd -pl user-management -am test` |
| Regenerate API code only | `./mvnw.cmd -pl api-contract install` |
| Skip formatter (emergency only) | add `-Dfmt.skip=true` |

- The formatter rewrites files during `validate`. Build before committing so the diff has no
  formatting noise.
- The runnable jar is `web-app/target/web-app-1.0.0-SNAPSHOT.jar`.
- Unit tests use JUnit 5 + Mockito + AssertJ (`spring-boot-starter-test`). Put time-based logic
  behind the injected `Clock` bean so it can be tested with a fixed clock (see `OtpServiceTest`).

## 7. Configuration

`application.yaml` holds everything shared and reads environment-specific values as `${NAME}`.
Those values come from `application-local.yaml` locally and from environment variables on servers.

| Key | Meaning |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET_KEY` | Base64 HMAC key, 32+ bytes. Different per environment. |
| `ACCESS_TOKEN_EXPIRY` / `REFRESH_TOKEN_EXPIRY` | Milliseconds (default 1 day / 30 days) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend URLs; wildcards like `https://*.vercel.app` work |
| `OTP_DEV_MODE` | `true` returns the OTP in the API response. **Must be `false` in production.** |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | First admin, created only when none exists |
| `security.public-access` | Paths reachable without a token. Everything else needs one. |
| `otp.*` | OTP length, validity, resend cooldown, hourly limit, max wrong attempts |

## 8. Database

- Database `studentportal`, schema `studentportal`.
- Tables are defined in `web-app/src/main/resources/schema/*.sql` and listed in
  `spring.sql.init.schema-locations`. **These scripts run on every startup**, so:
  - use `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`,
    `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`;
  - never put `DROP` or data-changing statements there;
  - don't use `DO $$ ... $$` blocks (Spring splits scripts on `;`).
- Hibernate runs with `ddl-auto: validate`. If an entity and its table don't match, the app refuses
  to start and tells you which column is wrong.
- Columns are `snake_case`; entity fields are `camelCase` (mapped automatically).
- Every entity extends `AuditInfo` so `created_at` / `last_updated_at` are filled automatically.
- Users are soft-deleted (`status = 'DELETED'`) so bookings and payments keep their history. Prefer
  soft delete for anything other records point to.

## 9. How a request flows

```
HTTP request
  -> JwtAuthenticationFilter     reads Bearer token, loads user, rejects blocked users
  -> Spring Security             public path? else must be authenticated
  -> Controller                  implements the generated *Api interface; @Admin/@Parent/@Tutor check
  -> Service                     business rules, @Transactional, throws ApiException subclasses
  -> Repository                  Spring Data JPA (Specification for search/filters)
  <- Mapper                      entity -> generated API model
  <- GlobalExceptionHandler      any exception -> {code, message, detail}
```

Rules that keep this clean:

- Controllers only translate between HTTP and the service. No business logic, no repositories.
- Throw `BadRequestException`, `NotFoundException`, `ConflictException`, `ForbiddenException`,
  `UnauthorizedException` or `TooManyRequestsException` from `common`. Don't add new handlers to
  `GlobalExceptionHandler` for module exceptions.
- Get the logged-in user with `CurrentUser.id()` / `CurrentUser.get()`. Never trust a user id sent in
  the request body for "my" data.
- Enum values in the API and in entities use the same names, so MapStruct maps them automatically.

## 10. Adding a new module

Example: `tutor-management`.

1. **Spec first.** Create `api-contract/src/main/resources/tutor-management.yaml` (copy the shape of
   `user-management.yaml`: tags, `bearerAuth`, `ErrorResponse`, pagination params). Share the Swagger
   link with the frontend before writing Java.
2. **Generator.** In `api-contract/pom.xml`, copy the `user-management-code-gen` `<execution>` and
   change `id`, `inputSpec`, `apiPackage` (`com.studentportal.api.tutormanagement`), `modelPackage`
   and `apiNameSuffix` (`TutorManagementApi`). Generated interfaces are named `{Tag}{Suffix}`, e.g.
   tag `Tutor Profile` → `TutorProfileTutorManagementApi`.
3. **Swagger.** Add an inner controller to `OpenApiConfig` (`/doc/v3/tutor-management`) and an entry
   under `springdoc.swagger-ui.urls` in `application.yaml`.
4. **Module.** Create `tutor-management/pom.xml` like `user-management/pom.xml` (depend on
   `api-contract`, `common`, and `user-management` if you need `CurrentUser` / role annotations).
   Add `<module>tutor-management</module>` to the root `pom.xml` and the dependency to
   `web-app/pom.xml`.
5. **Code.** Package `com.studentportal.tutormanagement` with `controller`, `service`, `repository`,
   `model/entity`, `mapper`. Entities extend `AuditInfo`, repositories extend `CoreRepository`.
6. **Tables.** Add `web-app/src/main/resources/schema/tutor.sql` and list it under
   `spring.sql.init.schema-locations` (after `users.sql` if it references `users`).
7. **Public endpoints** (e.g. tutor search for logged-out visitors) go in `security.public-access`.
8. **Tests** for every service rule, then a full `clean install`.

## 11. Auth and error codes (for frontend)

| Who | How they log in |
|---|---|
| Parent / Tutor | `POST /api/v1/auth/otp/send` `{mobileNumber}`, then `POST /api/v1/auth/otp/verify` `{mobileNumber, otp, role}`. `role` (PARENT/TUTOR) is only needed the first time; the account is created then and `newUser` is `true`. |
| Admin | `POST /api/v1/auth/signin` `{email, password}` |

- Send `Authorization: Bearer <accessToken>` on every protected call.
- Access token: 1 day. Refresh token: 30 days. On `401 TOKEN_EXPIRED`, call
  `POST /api/v1/auth/refresh` once and retry the request.
- After login, route by `user.role`; if `newUser` or `!user.profileComplete`, show onboarding.
- OTP rules: 6 digits, valid 5 minutes, resend after 30 seconds, max 5 per hour per number, 5 wrong
  attempts.
- Mobile numbers are 10 digits starting with 6–9, without `+91`.

Every error has the shape `{ "code": "...", "message": "...", "detail": { ... } }`. Switch on `code`,
never on `message`:

| HTTP | code | Frontend should |
|---|---|---|
| 400 | `VALIDATION_FAILED` | show `detail.<field>` under each field |
| 400 | `MALFORMED_REQUEST` | bug in the request body (bad JSON or enum value) |
| 400 | `ROLE_REQUIRED` | new number: ask Parent/Tutor and send verify again with the **same OTP** |
| 401 | `OTP_INVALID` | show `message` (includes attempts left) |
| 401 | `OTP_EXPIRED` / `OTP_ATTEMPTS_EXCEEDED` | show `message`, offer "Resend OTP" |
| 401 | `TOKEN_EXPIRED` | refresh, retry once |
| 401 | `AUTH_REQUIRED` / `TOKEN_INVALID` / `ACCOUNT_BLOCKED` | clear tokens, go to login |
| 401 | `INVALID_CREDENTIALS` | wrong admin email or password |
| 403 | `ACCOUNT_BLOCKED` / `ACCOUNT_DELETED` | show `message` on the login screen |
| 403 | `ACCESS_DENIED` | user's role can't open this |
| 404 | `NOT_FOUND` | record or endpoint doesn't exist |
| 409 | `CONFLICT` | mobile number or email already used |
| 429 | `TOO_MANY_REQUESTS` | show `message` (wait before resending) |
| 500 | `INTERNAL_ERROR` | generic error; check the server log |

## 12. API list

| Method | Path | Who |
|---|---|---|
| POST | `/api/v1/auth/otp/send` | public |
| POST | `/api/v1/auth/otp/verify` | public |
| POST | `/api/v1/auth/signin` | public |
| POST | `/api/v1/auth/refresh` | public |
| GET, PUT | `/api/v1/users/me` | any logged-in user |
| GET, POST | `/api/v1/users` | admin: list (search, role, status, paging, sort), create |
| GET | `/api/v1/users/summary` | admin: dashboard counts |
| GET, PUT, DELETE | `/api/v1/users/{id}` | admin: view, edit, soft delete |
| PATCH | `/api/v1/users/{id}/status` | admin: block (with reason), unblock |

Full request/response details are in Swagger.

## 13. Troubleshooting

| Problem | Fix |
|---|---|
| `NoSuchMethodError` from `fmt-maven-plugin` | You're building with JDK 25. Set `JAVA_HOME` to JDK 21. |
| IntelliJ: `cannot find symbol AuthenticationUserManagementApi` | Generated code is missing. Run `./mvnw.cmd -pl api-contract install`, then reload Maven. |
| `Schema-validation: missing column ...` on startup | The entity has a field the table doesn't. Add the column to the schema SQL with `ADD COLUMN IF NOT EXISTS`. |
| `password authentication failed for user "postgres"` | Wrong `DB_PASSWORD` in `application-local.yaml`. |
| `database "studentportal" does not exist` | Run `CREATE DATABASE studentportal;` once. |
| `Port 8080 was already in use` | Another app (maybe the ERP) is running. Stop it or start with `--server.port=8081`. |
| `mvnw.cmd` does nothing in Git Bash | Run it as `./mvnw.cmd`. |
| Frontend gets a CORS error | Add its URL to `CORS_ALLOWED_ORIGINS` and restart. |
| Frontend always gets 401 | Check the header is exactly `Authorization: Bearer <token>`, and that you're not sending the refresh token. |
