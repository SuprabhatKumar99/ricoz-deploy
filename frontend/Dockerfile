# ─── Stage 1: Build ──────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files first for layer caching
COPY package*.json ./
RUN npm install --no-audit --no-fund --prefer-offline

# Copy source and build
COPY . .
RUN npm run build:prod

# ─── Stage 2: Serve ──────────────────────────────────────────
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx files
RUN rm -rf ./*

# Copy built Angular app
COPY --from=builder /app/dist/ricozknow-frontend/browser .

# Copy custom Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Non-root user
RUN addgroup -S ricozknow && adduser -S ricozknow -G ricozknow \
    && chown -R ricozknow:ricozknow /usr/share/nginx/html \
    && chown -R ricozknow:ricozknow /var/cache/nginx \
    && chown -R ricozknow:ricozknow /var/log/nginx \
    && touch /var/run/nginx.pid \
    && chown ricozknow:ricozknow /var/run/nginx.pid

USER ricozknow

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
