# Arithmancer

- `backend/`: Spring Boot 4.1 (Java 21, Maven), built and run in Docker
- `frontend/`: React 19 + TypeScript, built with Vite

## Running locally

Start the game's container, which builds the frontend and serves it with the backend on http://localhost:8080:

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

On the VPS, the game listens on one port bound to 127.0.0.1 (8080 unless `.env` sets `PORT`): Spring Boot serves the built frontend, `/ws` and `/api` together. A Caddy installed on the VPS itself, shared with other projects, forwards a domain to it and handles HTTPS:

```
game.example.com {
	reverse_proxy localhost:8080
}
```

One-time setup on the VPS:

1. Install Docker, Git and curl.
2. Clone the repo into the home directory: `git clone https://github.com/Mr-coo/Arithmancer.git ~/arithmancer`
3. Optionally copy `.env.example` to `.env` in `~/arithmancer` and set `PORT`, if another project already uses 8080.
4. Run `docker compose up -d --build` in `~/arithmancer`.
5. Add the site above to the VPS's Caddyfile and reload Caddy.
6. Create an SSH key pair for deploys and append the public key to `~/.ssh/authorized_keys`. That user must be able to run `docker` (e.g. be in the `docker` group).
7. In GitHub, under Settings → Secrets and variables → Actions, add `VPS_HOST` (IP or domain), `VPS_USER` and `VPS_SSH_KEY` (the private key).

After that, every push to `master` that touches `backend/`, `frontend/` or `docker-compose.yml` runs the backend tests, then SSHes into the VPS, resets `~/arithmancer` to `origin/master`, runs `docker compose up -d --build` and waits for the game to answer on its port. It can also be run by hand from the Actions tab.

## Endpoints

- `GET /api/hello`: sample endpoint
- `GET /actuator/health`: health check
