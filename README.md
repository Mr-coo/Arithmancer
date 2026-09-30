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

The Vite dev server proxies `/api/*` and `/ws` to the backend on the VPS (43.156.104.167), so the frontend calls relative paths like `fetch('/api/hello')` and needs no CORS setup. To use the local backend container instead, point the targets in `frontend/vite.config.ts` at `localhost:8080`.

## Deploying

On the VPS, Caddy is the only public entry point (ports 80 and 443). The backend's port 8080 is bound to 127.0.0.1, so it can't be reached from outside.

One-time setup on the VPS:

1. Install Docker and Git.
2. Clone the repo into the home directory: `git clone https://github.com/Mr-coo/Arithmancer.git ~/Arithmancer`
3. Copy `.env.example` to `.env` in `~/Arithmancer`. `COMPOSE_PROFILES=prod` turns Caddy on. To use a domain, point its A record at the VPS and set `DOMAIN`; Caddy fetches an HTTPS certificate for it. Without `DOMAIN`, Caddy serves plain HTTP on the VPS IP.
4. Open ports 80 and 443 in the firewall.
5. Run `docker compose up -d --build` in `~/Arithmancer`.
6. Create an SSH key pair for deploys and append the public key to `~/.ssh/authorized_keys`. That user must be able to run `docker` (e.g. be in the `docker` group).
7. In GitHub, under Settings → Secrets and variables → Actions, add `VPS_HOST` (IP or domain), `VPS_USER` and `VPS_SSH_KEY` (the private key).

After that, every push to `master` that touches `backend/`, `caddy/` or `docker-compose.yml` runs the backend tests, then SSHes into the VPS, resets `~/Arithmancer` to `origin/master` and runs `docker compose up -d --build`. It can also be run by hand from the Actions tab.

## Endpoints

- `GET /api/hello`: sample endpoint
- `GET /actuator/health`: health check
