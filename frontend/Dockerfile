# ---- Build stage: type-check and bundle the app into static files ----
FROM node:24.22.0-alpine AS build
WORKDIR /app

# Install dependencies first so they are cached between source changes.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# Tests run in the developer/CI build, not in the image build.
RUN npm run build

# ---- Runtime stage: nginx as a non-root user ----
FROM nginxinc/nginx-unprivileged:1.30.6-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
