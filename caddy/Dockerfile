# syntax=docker/dockerfile:1

FROM node:24-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend ./
RUN npm run build

FROM caddy:2-alpine
# Baked in rather than mounted, so `docker compose up --build` picks up Caddyfile changes
COPY caddy/Caddyfile /etc/caddy/Caddyfile
COPY --from=frontend /app/dist /srv
