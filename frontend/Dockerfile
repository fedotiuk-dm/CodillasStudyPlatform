# Build context is the repo ROOT so the build can read backend/openapi for the Orval client.
# Build:  docker build -f frontend/Dockerfile -t codillas-frontend .
FROM node:24-alpine AS build
RUN corepack enable
WORKDIR /app/frontend

# Install deps first (cached layer).
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
# --ignore-scripts: skip native postinstalls (esbuild/sharp/swc ship prebuilt musl binaries via
# platform optional deps), which also avoids pnpm's non-interactive "ignored builds" hard error.
RUN pnpm install --frozen-lockfile --ignore-scripts

# Sources + the OpenAPI specs the client is generated from.
COPY frontend ./
COPY backend/openapi /app/backend/openapi

# Generate the API client, then build (direct bins skip pnpm's run-time deps check).
RUN ./node_modules/.bin/orval && ./node_modules/.bin/next build

FROM node:24-alpine AS run
ENV NODE_ENV=production
WORKDIR /app
COPY --from=build /app/frontend/.next/standalone ./
COPY --from=build /app/frontend/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
