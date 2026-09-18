# RicozKnow Frontend — rebuilt

This project was rebuilt from the supplied Spring Boot backend and the supplied RicozKnow UI/UX instructions.

## Grounded backend endpoints

The backend contains these controller surfaces:

- `/api/v1/auth/login`
- `/api/v1/auth/register`
- `/api/v1/tenants/lookup`
- `/api/v1/articles/**`
- `/api/v1/categories/**`
- `/api/v1/assets/**`
- `/api/v1/search`
- `/api/v1/admin/search/reindex`
- `/api/v1/admin/synonyms/**`
- `/api/v1/users/**`
- `/api/v1/analytics/**`
- `/api/v1/admin/branding`
- `/api/v1/portal/**`
- `/api/v1/agent/**`

The article create/update models use `JsonNode content`, so the editor parses JSON before sending it. It does not invent a separate rich-text payload contract.

## Tenant / subdomain behavior

The supplied backend resolves anonymous portal tenant context from the hostname using `ricozknow.tenant.base-domain`. Authenticated API requests resolve the tenant from the authenticated principal.

The frontend therefore:

1. Uses the tenant slug explicitly for `/api/v1/auth/login`.
2. Attempts `/api/v1/tenants/lookup?subdomain=<host-subdomain>` on the portal when the browser is on a multi-part hostname.
3. Does not send a tenant ID to protected endpoints because the backend derives it from the authenticated principal.

For local development, the API defaults to:

`http://localhost:8080`

You can override it before starting the app:

```js
localStorage.setItem("rk_api_url", "http://localhost:8080");
```

The supplied backend CORS configuration allows the Angular development origin `http://localhost:4200`, so use that origin unless you change the backend CORS configuration.

## UI/UX implementation

The supplied design instructions call for:

- Light, content-focused enterprise UI.
- Search-first customer portal.
- Persistent studio sidebar and topbar.
- Action-oriented dashboard.
- Table-first article management.
- Dedicated review queue.
- Version history.
- Simple administration.
- Responsive layouts.
- Keyboard/focus accessibility.
- Loading/error/empty states.
- Reduced motion support.

The implementation uses shared `MetricCard`, `Status`, and `EmptyState` UI components and route-level lazy loading.

## Run

```bash
npm install
npm start
```

Open:

`http://localhost:4200`

## Important scope note

The supplied backend does not expose a dedicated endpoint for article reading-time calculation, related-article recommendations, keyword management, a server-side article preview endpoint, or a separate review-queue endpoint. The frontend does not invent those APIs.

The review queue is assembled from the existing article list and version-history endpoints, filtering versions whose status is `IN_REVIEW`.

The article editor intentionally uses a JSON editor because the backend's article contract is `JsonNode content`. A visual rich-text schema was not present in the supplied backend.

## Authentication refresh

The backend exposes `/api/v1/auth/refresh` and `/api/v1/auth/logout` and stores the refresh token in the `rk_refresh_token` HttpOnly cookie. The frontend therefore never stores or reads the refresh token in JavaScript. Login/refresh/logout use `withCredentials`, and the auth interceptor refreshes the access token once after a `401` response before retrying the original request. Concurrent refresh attempts are shared through a single in-flight request.

For local HTTP development, the backend configuration uses `REFRESH_COOKIE_SECURE=false` by default. Production should set `REFRESH_COOKIE_SECURE=true` and use HTTPS.

## Review queue content

The review queue renders the structured article `content.blocks` payload as rich content instead of displaying its raw JSON. It supports the backend-validated block types: heading, paragraph, image, list, and code.
