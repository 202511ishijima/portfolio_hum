# portfolio-templates

This repository contains:
- a Spring Boot backend (`spring-app`) using MyBatis + Thymeleaf
- static frontend files served locally with VS Code Live Server

## Project Structure

- `spring-app/` : backend application (Maven)
- `pages/`, `products/`, `assets/` : static frontend
- `Dockerfile` : Render-ready multi-stage Docker build
- `.dockerignore` : Docker build exclusions

## Local Development

### 1. Start backend

```powershell
cd spring-app
.\mvnw.cmd spring-boot:run
```

- Backend URL: `http://localhost:8080`
- Root `/` redirects to `/admin/dashboard`

### 2. Start frontend

This project does not require a Python HTTP server.
Use VS Code Live Server for static files.

Examples:
- `http://127.0.0.1:3000/index.html`
- `http://127.0.0.1:3000/pages/cafe-order.html?session=<session-id>`

## Main Admin URLs

- `http://localhost:8080/admin/dashboard`
- `http://localhost:8080/admin/employees`
- `http://localhost:8080/admin/shifts`
- `http://localhost:8080/admin/products/stocks`
- `http://localhost:8080/admin/cafe/reception`
- `http://localhost:8080/admin/cafe/customer-screen`
- `http://localhost:8080/admin/cafe/orders`

## Database (default)

Defined in `spring-app/src/main/resources/application.yml`:

- DB: H2 file database
- URL: `jdbc:h2:file:./data/portfolio_backend;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`
- H2 Console: `http://localhost:8080/h2-console`

Environment variables supported:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Test

```powershell
cd spring-app
.\mvnw.cmd test
```

## Render Deployment (Docker)

Render deployment is prepared in this repository:
- Multi-stage build (`build` + `runtime`)
- Maven Wrapper build
- `chmod +x ./spring-app/mvnw` included for Linux permission safety
- Runtime port fixed to `10000` with `-Dserver.port=10000`

### Local Docker build check

```powershell
docker build -t portfolio-render-test .
```

If Docker Desktop/daemon is not running, this command fails.

### Render settings

- Environment: `Docker`
- Root directory: repository root (where `Dockerfile` exists)
- Port: `10000`
- Optional environment variables:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`

## Notes

- If `mvnw Permission denied` occurs, check Dockerfile includes `chmod +x`.
- To avoid BOM compile errors (`illegal character: '\ufeff'`), keep Java files as UTF-8 without BOM.
- Local `*.mv.db` / `*.trace.db` files are development data.
