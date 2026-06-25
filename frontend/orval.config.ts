import { type Config, defineConfig } from "orval";

// API-first: generate the client from the hand-written specs in backend/openapi (the single source
// of truth). Orval resolves the cross-file $refs (paths → schemas → common) directly, so NO bundling
// step is needed. Auth + base URL + FormData handling live in the axios mutator. Zod schemas are
// generated per module (for react-hook-form resolvers) except where a module has no request bodies
// worth validating (read-only, or live messaging).
const SPECS = "../backend/openapi";
const MUTATOR = { path: "./lib/services/axios-instance.ts", name: "customInstance" };

const ZOD_DISABLED = new Set(["gradebook", "chat", "notification"]);

const MODULES = [
  "user",
  "course",
  "enrollment",
  "homework",
  "assessment",
  "gradebook",
  "chat",
  "notification",
  "files",
] as const;

function moduleConfig(module: string): Config {
  const input = { target: `${SPECS}/${module}-paths.yaml` };

  const config: Config = {
    [module]: {
      input,
      output: {
        mode: "tags-split",
        target: `./lib/api/${module}`,
        schemas: `./lib/api/${module}/model`,
        client: "react-query",
        httpClient: "axios",
        clean: true,
        override: {
          mutator: MUTATOR,
          query: { signal: true },
        },
      },
    },
  };

  if (!ZOD_DISABLED.has(module)) {
    config[`${module}Zod`] = {
      input,
      output: {
        mode: "tags-split",
        target: `./lib/api/${module}/zod`,
        client: "zod",
        fileExtension: ".zod.ts",
      },
    };
  }

  return config;
}

export default defineConfig(Object.assign({}, ...MODULES.map(moduleConfig)));
