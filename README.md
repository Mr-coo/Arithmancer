# Arithmancer

- `backend/`: Spring Boot 4.1 (Java 21, Maven), built and run in Docker
- `frontend/`: React 19 + TypeScript, built with Vite

## Running locally

Start the game's container, which builds the frontend and serves it with the backend on http://localhost:8080. It joins the `caddy_net` network the VPS's Caddy uses, so create that once first:

```sh
docker network create caddy_net
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

On the VPS, Spring Boot serves the built frontend, `/ws` and `/api` together on port 8080. A Caddy gateway shared with other projects runs in Docker on the `caddy_net` network, where the game is `arithmancer`, and forwards the domain to it with HTTPS:

```
arithmancer.marcolinardi.site {
	reverse_proxy arithmancer:8080
}
```

The game also listens on 127.0.0.1 of the VPS (port 8080 unless `.env` sets `PORT`), for the deploy's check.

One-time setup on the VPS:

1. Install Docker, Git and curl.
2. Clone the repo into the home directory: `git clone https://github.com/Mr-coo/Arithmancer.git ~/arithmancer`
3. Optionally copy `.env.example` to `.env` in `~/arithmancer` and set `PORT`, if another project already uses 8080.
4. Create the shared network if the Caddy gateway has not already: `docker network create caddy_net`.
5. Run `docker compose up -d --build` in `~/arithmancer`.
6. Add the site above to the gateway's Caddyfile and reload Caddy. Open ports 80 and 443 in the firewall.
7. Create an SSH key pair for deploys and append the public key to `~/.ssh/authorized_keys`. That user must be able to run `docker` (e.g. be in the `docker` group).
8. In GitHub, under Settings → Secrets and variables → Actions, add `VPS_HOST` (IP or domain), `VPS_USER` and `VPS_SSH_KEY` (the private key).

After that, every push to `master` that touches `backend/`, `frontend/` or `docker-compose.yml` runs the backend tests, then SSHes into the VPS, resets `~/arithmancer` to `origin/master`, runs `docker compose up -d --build` and waits for the game to answer on its port. It can also be run by hand from the Actions tab.

## Endpoints

- `GET /api/hello`: sample endpoint
- `GET /actuator/health`: health check
