# Student Portal - Backend

Personal tutor marketplace. Parents search and book tutors for home or online tuition; tutors manage
their profile, classes and earnings; admins verify tutors and run the platform.

**Stack:** Java 21 · Spring Boot 3.2.0 · PostgreSQL · OpenAPI/Swagger · JWT + mobile OTP

## Docs

| Doc | What's in it |
|---|---|
| [PROJECT_GUIDE.md](PROJECT_GUIDE.md) | Project structure, setup, run, build, test, configuration, database rules, adding a module, auth flow and error codes for frontend, troubleshooting |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branch names, commit messages, PR titles and template, review and merge rules, releases |

## Quick start

```bash
# 1. Create the database once (psql / pgAdmin):  CREATE DATABASE studentportal;
# 2. Local config (git-ignored) - then fill in DB_PASSWORD, JWT_SECRET_KEY, BOOTSTRAP_ADMIN_PASSWORD
cp web-app/src/main/resources/application-local.example.yaml web-app/src/main/resources/application-local.yaml
# 3. Build with JDK 21
JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw.cmd clean install
# 4. Run
"/c/Program Files/Java/jdk-21/bin/java" -jar web-app/target/web-app-1.0.0-SNAPSHOT.jar
```

Swagger UI: http://localhost:8080/swagger-ui/index.html

In IntelliJ: open this folder as a Maven project, set the SDK to JDK 21, and run `WebAppApplication`.

## Modules

| Module | Purpose |
|---|---|
| `api-contract` | OpenAPI specs; controller interfaces and models are generated from them |
| `common` | Base entity, repository, pagination, exceptions |
| `user-management` | OTP/password login, JWT security, profile, admin user management |
| `web-app` | Runnable app, global error handling, config, database scripts |
