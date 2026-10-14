# syntax=docker/dockerfile:1
# ============================================================================
#  StayEase web image: React production build served by Nginx.
#  Build context = repository root (so ./nginx can be copied):
#      docker build -f frontend/Dockerfile .
# ============================================================================

# ---- 1. build the React app (dev dependencies stay in this stage) ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm test && npm run build

# ---- 2. runtime: nginx + static files only -----------------------------------
FROM nginx:stable-alpine AS runtime
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/cloudflare-realip.conf /etc/nginx/cloudflare-realip.conf
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
# The certificate is mounted at run time (compose.yaml): /etc/nginx/certs/origin.pem + origin.key
