# Frontend: visual refine + richer data states (design)

Date: 2026-06-29
Scope: `frontend/` — finish the visual layer the dashboard-refresh commit (`d075ecf`) started.
**Refine** the existing identity (violet + Geist, no rebrand) and upgrade loading/error/empty
states to the boosting `PageBoundary`/`StateMessage` discipline, on Codillas tokens.
Owner: @fedotiuk-dm. Branch: `feat/lms-hardening`, frontend commits per phase.

> Direction chosen by the user: **refine, not rebrand**. Keep the soul, finish the craft.
> Surfaces: dashboard `views/*` + shell (sidebar/topbar/bell) + marketing/landing/login.

## What's already good (don't rebuild)

- OKLCH light/dark token system with semantic `success/warning/info` + sidebar tokens, radius scale,
  gradient bg, view-transitions.
- Shared atoms adopted: `DataState` (12 views), `PageHeader` (13), `StatusBadge`, `ConfirmDialog`,
  `FormDialog`, `Wizard`, etc.
- `StatusBadge` map is **complete** for every real lifecycle status — verify only.
- `Button` already has good `focus-visible` rings + hover-lift.
- Almost zero drift: one off-token color, zero stray `confirm()`.

## Gaps this addresses

1. **No font is wired** — `layout.tsx` uses no `next/font`; the app renders in the OS `system-ui`
   fallback. "Keep Geist" → actually wire Geist. Biggest single craft win.
2. **`DataState` is thin** — boolean flags, one generic skeleton, **no retry**, **no ErrorBoundary**
   (a render error in children white-screens the page).
3. **Untokenized craft** — card elevation is an inline `shadow-[…]`; no motion tokens.
4. **System not written down** — `frontend/AGENTS.md` has no design section → drift risk.

## Plan (phased, one commit each)

### A. Foundation — font + tokens
- `pnpm add geist`; wire `GeistSans` in root `layout.tsx`; map `--font-sans → --font-geist-sans` in
  `globals.css` so everything inherits Geist (Tailwind v4 `@theme inline`).
- Add `--shadow-card` elevation token + `--duration-fast/base` + `--ease` motion tokens; apply the
  shadow token in `card.tsx` (replaces the inline shadow). Leave the sidebar inset ring (purposeful).
- Skip custom type-scale tokens — Tailwind's `text-sm…3xl` defaults already are the scale.

### B. `DataState` upgrade (the explicit ask) — boosting discipline, our tokens
- `pnpm add react-error-boundary`.
- Keep the **boolean API** (`isLoading/isError/isEmpty`) so all 12 call sites keep working.
- Add: wrap children in **`ErrorBoundary` + `resetKeys`** (render errors show the error state);
  optional **`onRetry`** → "try again" button (wired to `refetch`); **`variant?: "list"|"table"|
  "cards"|"detail"`** → skeleton shaped to the content; icon-ring error/empty surface on tokens
  (NOT boosting's hardcoded `#ff8a8e`). New i18n key `common.retry` in uk/en/de.

### C. Sweep
- `group-detail-dialog.tsx:269` `text-green-600` → `text-success`.
- Verify `StatusBadge` coverage (expected: no change).

### D. Document + apply
- Add a **design-system section** to `frontend/AGENTS.md`: tokens (color/tone/elevation/motion),
  the type/font rule, shared atoms, the `DataState` state rule.
- Apply `variant`/`onRetry` to high-traffic views (courses, homework, gradebook) as the worked
  example; the rest follow the documented pattern. Confirm shell + marketing/login inherit the font
  and pass a focus/dark-mode glance.

## Verify (per phase)
`pnpm type-check` (tsc --noEmit) green; `pnpm lint` (biome) clean; manual smoke on a running
`pnpm dev` — **never `next build` while dev is up**. React Compiler on → no hand `useMemo/useCallback`.

## Out of scope (YAGNI)
- Rebrand / new palette / display typeface (user chose refine).
- A frontend test runner.
- Boosting's `PAGE_STATUS` enum + `createDataAccessor`/`PageShell` machinery — keep our boolean
  `DataState`; adopt the *discipline* (ErrorBoundary, retry, shaped skeletons), not the machinery.
- Rewriting all 12 view skeletons at once — document the pattern, convert the high-traffic ones.

## Decisions log
| Decision | Choice | Rationale |
|---|---|---|
| Identity | Refine, no rebrand | User's pick |
| Font | Wire Geist (`geist` pkg) | "Keep Geist" → make it real; canonical Next pairing |
| State machinery | Evolve `DataState`, keep boolean API | Non-breaking for 12 call sites; ponytail |
| ErrorBoundary | `react-error-boundary` pkg | Standard, tiny, boosting uses it |
| Skeleton shapes | `variant` prop, opt-in | Shaped skeletons read "finished"; default stays generic |
| Commits | Per phase on `feat/lms-hardening` | User reviews via git history |
