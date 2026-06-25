import { defineConfig } from "orval";

// API-first: generate the client from the hand-written specs in backend/openapi (the single source
// of truth — same files the backend generates from), NOT the running /v3/api-docs. Add one entry
// per module spec. Pagination params + error bodies come from common.yaml via $ref, so there's no
// per-endpoint boilerplate.
const SPECS = "../backend/openapi";

const client = (module: string) => ({
  input: { target: `${SPECS}/${module}-paths.yaml` },
  output: {
    mode: "tags-split" as const,
    target: `./lib/api/${module}`,
    schemas: `./lib/api/${module}/model`,
    client: "react-query" as const,
    baseUrl: "http://localhost:8081",
    clean: true,
  },
});

export default defineConfig({
  course: client("course"),
});
