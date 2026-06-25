# Codillas Frontend

Next.js (App Router) + Orval-generated API client. Admin is role-gated routes
(`/admin/*` behind `ROLE_ADMIN`) in this same app — no separate admin stack.

```bash
pnpm install
pnpm dev          # http://localhost:3000
pnpm api          # regenerate client from http://localhost:8081/v3/api-docs (backend must be up)
```

Generated client lands in `lib/api/` (gitignored-friendly to regenerate).
