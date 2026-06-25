import { defineConfig } from "orval";

// Generates the API client from the backend's aggregated OpenAPI doc.
// Backend must be running: http://localhost:8081/v3/api-docs
export default defineConfig({
  codillas: {
    input: { target: "http://localhost:8081/v3/api-docs" },
    output: {
      mode: "tags-split",
      target: "./lib/api/generated",
      schemas: "./lib/api/model",
      client: "react-query",
      baseUrl: "http://localhost:8081",
      clean: true,
    },
  },
});
