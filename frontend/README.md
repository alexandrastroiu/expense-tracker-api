# Spendly frontend

A responsive React + TypeScript dashboard for the existing Expense Tracker API. Neutral surfaces, muted green accents, and light/dark themes keep the interface simple. The theme follows the system preference initially, and the toggle remembers your choice.

All additions are contained in `frontend/`. No existing backend, root documentation, Compose, or CI files need edits.

## Local development

Requires Node.js 22 and npm. Start your existing backend using its documented setup, then run:

```bash
cd frontend
npm ci
npm run dev
```

Open http://127.0.0.1:5173. The development server proxies `/api` to `http://localhost:8080`, so browser requests stay on the same origin and no backend CORS change is required.

For a different backend, copy `.env.example` to `.env.local`, change `API_PROXY_TARGET`, and restart the frontend. This value is server-side proxy configuration; do not put secrets in browser environment variables.

## Features

- Username/password login and registration with backend field constraints.
- Logout, automatic session expiry, and return to login on authenticated `401` responses.
- Monthly budget creation, editing, deletion, and the backend's monthly spending summary.
- Expense and recurring-payment creation, editing, and confirmed deletion.
- Separate search and range-filter modes matching the controller contracts.
- Server-side, zero-based pagination, page-size selection, and validated sort fields with `id,desc` as a stable tie-breaker.
- Empty, loading, error, retry, and save-confirmation states.
- Responsive layout, labeled controls, native modal dialogs, and light/dark mode.

The overview shows the five latest expenses across all dates. The month selector controls only the budget summary. Amounts have two decimal places and no currency symbol because the API does not store currencies. Recurring amounts are labeled per payment; monthly estimates come from the backend.

A budget must exist for a month before the API can return its summary. A `404` on that summary shows the set-budget prompt.

## Authentication behavior

The API returns `{ "token": "..." }` from login, and registration returns an empty `201` response. After registration, the user signs in explicitly.

The frontend stores the JWT in `sessionStorage`, allowing reloads within a tab. Each protected request includes `Authorization: Bearer <token>`. The JWT expiry claim schedules local logout; the backend remains responsible for validating token signatures and access rights. Expired or rejected tokens return the user to login. There is no refresh endpoint, so users sign in again when the token expires.

**Logout clears the browser session and all mounted account data. It does not revoke the JWT on the server.** The backend is stateless and has no logout/revocation endpoint, so a previously copied token remains valid until expiry. Browser token storage is accessible to JavaScript; a future backend change could use an HttpOnly cookie and server-supported revocation if needed.

## Endpoint integration

| UI | API |
| --- | --- |
| Register / sign in | `POST /api/auth/register`, `POST /api/auth/login` |
| Categories | `GET /api/categories` |
| Expenses | `GET/POST /api/expenses`, `PUT/DELETE /api/expenses/{id}` |
| Expense search | `GET /api/expenses/search` with title, categoryId, expenseDate |
| Expense ranges | `GET /api/expenses/filter` with categoryId, minAmount, maxAmount, start, end |
| Recurring payments | `GET/POST /api/recurring-expenses`, `PUT/DELETE /api/recurring-expenses/{id}` |
| Recurring search | `GET /api/recurring-expenses/search` with title, categoryId, frequency |
| Recurring ranges | `GET /api/recurring-expenses/filter` with required minAmount and maxAmount |
| Budget summary | `GET /api/budgets/summary?period=YYYY-MM-01` |
| Budget changes | `POST /api/budgets`, `PUT/DELETE /api/budgets/{id}` |

List responses consume the Spring Page fields `content`, `number`, `size`, `totalElements`, and `totalPages`. Search and range filters use separate requests because the existing API exposes them separately. No totals are inferred from the current table page.

## Checks and build

```bash
npm test
npm run build
```

Tests cover API query construction, pagination, sort tie-breakers, JWT expiry decoding, bearer headers, empty delete responses, empty security-filter `401` responses, and backend validation messages. The build runs strict TypeScript checking and creates `dist/`.

`npm run preview` previews the compiled static assets only. It does not provide the development API proxy; use the Docker setup below to test a complete production-style deployment.

## Docker deployment

From the **repository root**, use the frontend Compose overlay alongside your existing file:

```bash
docker compose -f docker-compose.yml -f frontend/compose.yaml up --build -d --wait
```

Open http://localhost:3000. The image builds the frontend and serves it with Nginx, which proxies `/api/` to `backend:8080` on the Compose network. The backend service name is already present in your root Compose file. The frontend includes a health check, security headers, and a same-origin Content Security Policy.

Stop using the same file combination:

```bash
docker compose -f docker-compose.yml -f frontend/compose.yaml down
```

For public hosting, route HTTPS through your hosting provider/reverse proxy. If the API runs outside this Compose network, update the **new frontend** Nginx proxy configuration for its hostname. The frontend bundle contains no credentials.

## CI integration when you are ready

Your existing workflow is untouched. Add a frontend job yourself with Node.js 22, then run these commands from `frontend/`:

```bash
npm ci
npm test
npm run build
```

The committed lockfile makes installs reproducible. `dist/` is the static build artifact. A future deployment job can build `frontend/Dockerfile` and publish its image after checks pass.

## Verification limits

The production build and API-contract unit tests were run successfully. Visual/browser QA was left to the project owner. Live authentication and CRUD were not tested against a running backend during implementation.
