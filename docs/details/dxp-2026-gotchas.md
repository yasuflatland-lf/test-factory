# DXP 2026 Runtime Gotchas

L3 detail. Runtime / auth / JSONWS / HTTP-client traps discovered during the DXP 2026.q1.3-lts migration that do not naturally live in the architectural ADR (`docs/ADR/adr-0008-dxp-2026-migration.md`) or the Gradle-flow doc (`docs/details/testing-gradle.md`). Read on demand from `.claude/rules/debugging.md`.

This is DXP-2026-specific. CE 7.4 counterparts (where they exist) live in `docs/details/api-liferay-ce74.md`.

## Baked-in `portal-liferay-online-config.properties` blocks JSONWS

`portal-impl.jar` in the DXP 2026 Docker image ships a `portal-liferay-online-config.properties` inside `WEB-INF/classes/` that sets `json.servlet.hosts.allowed=N/A`. The property loader's last-wins rule means this value wins over anything set in `portal-ext.properties`, and any Basic Auth request to `/api/jsonws/*` or `/o/*` returns HTTP 403 with an empty body (`{}`) before the auth-verifier pipeline even fires. CE 7.4 does not ship this file.

**Fix (workspace-native flow):** place an empty `portal-liferay-online-config.properties` under `configs/common/` — the workspace plugin's image entrypoint copies it into Liferay Home before startup, shadowing the baked-in copy. Complementary: `configs/common/portal-ext.properties` must contain `json.servlet.hosts.allowed=` (explicit empty) so nothing ever re-installs `N/A`. Refs: commits `3f7e465`, `7bb05fc`, `95b19b5`.

## BasicAuth `default_urlsIncludes` config-override needs a glob for each prefix

DXP 2026's `BasicAuthHeaderAuthVerifierConfiguration` `default_urlsIncludes` is matched with a `PortletRequestMatcher`-style ant-path glob, not a prefix. The value `/xmlrpc*` does NOT match `/xmlrpc/rpc-call`; it only matches paths whose last segment literally starts with `xmlrpc`. Every entry must end in `/*` when the desired match is "this prefix and everything under it":

```
configuration.override.com.liferay.portal.security.auth.verifier.internal.basic.auth.header.configuration.BasicAuthHeaderAuthVerifierConfiguration~default_urlsIncludes="/api/*,/portal/api/*,/o/*,/xmlrpc/*"
```

Must match the companion property `auth.verifier.BasicAuthHeaderAuthVerifier.urls.includes` character-for-character — divergence between the two sources causes the auth-verifier to silently admit only a subset. Ref: commit `6807927`.

## AuthVerifierPipeline failure-cache has a ~3-minute refresh interval

DXP 2026's `AuthVerifierPipeline` caches failed-auth results. While the cache holds a stale `passwordReset=true` verdict, hammering `_checkBasicAuth()` every second keeps the entry alive — the cache is refreshed on its own timer, not on access. Observed: ~180 s on a cold container, up to 300 s in the worst case. Rapid probing against a 403 that you have already "fixed" at the DB layer will not flip green until that timer fires.

**Fix:** probe with Basic Auth **after dropping the form-login cookies** — a fresh session forces DXP to evaluate the Basic Auth credentials against the current DB state rather than reuse the cached session verdict. Empirically this removes the 3-minute wait and JSONWS turns green within seconds of the `update_password` POST. Poll cap in `BaseLiferaySpec` is still 30 s as a safety margin. Refs: commits `85f0d4f`, `b40dc8c`, `5ad8d42`.

## `_waitForLicenseActivated` checks the redirect, not the status code

DXP 2026 returns HTTP **200** for `/c/portal/login` even while the license chain is still composing — the login form template is rendered inline under that URL. Polling on status code will exit early, before the license is actually activated, and the subsequent update-password flow will race against the license composition. The reliable signal is the `Location` response header on `GET /sign-in` (with `instanceFollowRedirects=false`): it stays `.../c/portal/license_activation` until activation completes, then disappears. Cap: 120 s. Ref: commit `8efac1e`.

## Admin bootstrap HTTP client must force `Accept-Encoding: identity`

Java's `HttpURLConnection` advertises `Accept-Encoding: gzip` by default but does NOT auto-decode the response. Groovy's `conn.inputStream.text` then decodes the gzipped bytes as UTF-8, silently corrupting the HTML (you get a readable preview that still fails regex matching for `p_auth`, `ticketId`, etc.). Every `GET`/`POST` in the bootstrap flow must set `Accept-Encoding: identity`.

Additionally, read the body through a byte-stream `ByteArrayOutputStream` loop and decode once at the end — `.text` on a large DXP HTML page has been observed to truncate under load. A dedicated `_readAll(InputStream)` helper replaces `.text` on both success and error streams. Refs: commits `ebbd81d`, `390c290`.

## `p_auth` is optional on the login POST; always present on update_password POST

DXP 2026's `LoginFilter` treats `/c/portal/login` as an anonymous public path — the login POST succeeds with or without `p_auth` in the query string. Bootstrap code should still best-effort extract a token from the landing page in case a future DXP release tightens this, but MUST NOT hard-fail when extraction returns null — that blocks the login path unnecessarily on cold-start pages that do not embed a token. The follow-up `POST /c/portal/update_password` does require `p_auth`, which is reliably present in the hidden form field on the ticket page. Ref: commit `f557615`.

## `p_auth` extraction needs fallback paths on cold containers

On the very first container start, `/sign-in` occasionally renders a minimal shell with no `p_auth` in the HTML. The bootstrap tries `/sign-in` → `/c` → `/` in order, and additionally scans each body for the JS-block form `"authToken":"..."` / `Liferay.authToken = "..."` as a second-pass fallback. Single-source extraction from `/sign-in` fails intermittently; the three-path sweep has not been observed to fail. Ref: commit `3674f0d`.

## Playwright alert-danger regex must be class-order-agnostic

DXP 2026 renders server-side validation errors with the class attribute order `class="alert alert-danger fade show"` on some pages and `class="alert-danger alert"` on others. The regex that detects a server-side error in `BaseLiferaySpec._assertNoAlertDanger` must match `alert-danger` anywhere inside the `class="..."` attribute, not require it after the literal `alert` token:

```groovy
/class\s*=\s*"[^"]*alert-danger[^"]*"[^>]*>([\s\S]{0,400}?)</
```

Ref: commit `39ccb26`.

## `loginAsAdmin` must fail loudly when every password candidate is rejected

`loginAsAdmin(pw)` tries each password in `[activePassword, NEW_PASSWORD, DEFAULT_ADMIN_PASSWORD]` and records the first one that yields a logged-in state. If none succeeds, the method used to return silently with `loggedInPassword=null` — every downstream test then failed on a baffling "not logged in" assertion. Raising `IllegalStateException` immediately localises the failure to bootstrap-state drift and the password list. Ref: commit `39ccb26`.

## `bnd.bnd` must exclude `javax.servlet` from `Import-Package` on DXP 2026

DXP 2026 resolves `jakarta.servlet.*` for new code, but transitive references pulled in through older libraries can still emit `javax.servlet` into the auto-generated `Import-Package`. Because the DXP framework does not export `javax.servlet`, the bundle then stays in `Installed` state with `Unresolved requirements: [osgi.wiring.package; (osgi.wiring.package=javax.servlet)]`.

Add to `bnd.bnd`:

```
Import-Package:\
	!javax.servlet,\
	!javax.servlet.http,\
	*
```

Ref: commit `423924e`.

## Workspace plugin ≥ 16.x needs explicit module discovery

The workspace plugin 16.0.5 (used for DXP 2026) does not auto-discover `modules/liferay-dummy-factory` when `settings.gradle` only declares the `integration-test` subproject. Two settings are required together: `liferay.workspace.modules.dir=modules` in `gradle.properties` AND an explicit `include 'modules:liferay-dummy-factory'` in `settings.gradle`. Omitting either one makes `./gradlew :modules:liferay-dummy-factory:jar` fail with "Project 'modules:liferay-dummy-factory' not found". Ref: commit `2d006cd`.

## Headless portal-instances list endpoint lags new companies

`GET /o/headless-portal-instances/v1.0/portal-instances` returns a paginated list that is cached via `PortalInstancePool`'s iterator. After creating a company, the list may not include it for several seconds even though the path-based getter `/o/headless-portal-instances/v1.0/portal-instances/{webId}` returns 200 immediately. Use the path-based getter for post-condition assertions on newly-created companies.

Field names on the returned payload: `portalInstanceId` (the webId string), `companyId` (the numeric PK). Do NOT read `.webId` — the field is not exposed. Refs: commits `05a992c`, `d528089`.
