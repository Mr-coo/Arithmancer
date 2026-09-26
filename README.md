# Arithmancer

- `backend/`: Spring Boot 4.1 (Java 21, Maven), built and run in Docker
- `frontend/`: React 19 + TypeScript, built with Vite

## Running locally

Start the backend container (it serves on http://localhost:8080):

```sh
docker compose up --build
```

Start the frontend dev server (it serves on http://localhost:5173):

```sh
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api/*` to the backend, so the frontend calls relative paths like `fetch('/api/hello')` and needs no CORS setup.

## Endpoints

- `GET /api/hello`: sample endpoint
- `GET /actuator/health`: health check
