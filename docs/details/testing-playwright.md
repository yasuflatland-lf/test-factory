# Playwright — concrete details

L3 detail. Source of truth for Playwright-specific selector patterns, version pinning, and headless gotchas. Read on demand from `.claude/rules/testing.md` or `.claude/rules/debugging.md`.

## Browser tests

- Runs **headless Chromium** via `PlaywrightLifecycle`.
- Login credentials: `test@liferay.com` / `test` (Liferay default admin).
- Navigate to portlets via **direct URL** with the portlet ID in the query string (`p_p_id=...&p_p_lifecycle=0`). Do not click through menus — direct navigation is faster and more reliable.
- Use CSS selectors for locators (`#count`, `.alert-success`, `button.btn-primary`, `[type=submit]`).
- Set explicit timeouts on waits: `waitForURL(..., new Page.WaitForURLOptions().setTimeout(30_000))`, `waitFor(new Locator.WaitForOptions().setTimeout(15_000))`.
- Close the `PlaywrightLifecycle` instance in `cleanupSpec()` using safe-navigation: `pw?.close()`.

## Playwright Java vs Node version skew

Playwright Java (`com.microsoft.playwright:playwright` on Maven Central) and Playwright Node/CLI (`playwright` on npm) ship on **separate release cycles**. The same `1.x.y` number can exist on one side and not the other.

As of 2026-04, npm publishes `1.59.1` as the latest stable, while Maven Central's latest is `1.59.0` — `1.59.1`, `1.59.2`, and `1.60.x` all return HTTP 404 from the Maven repo.

The Playwright project recommends keeping the **client (Java) and driver (CLI) on the same version**, so bumping one side ahead of the other invites protocol skew and must be avoided.

Before bumping the Java side, **always confirm the version actually exists on Maven Central** with a direct POM fetch:

```bash
curl -s -o /dev/null -w "%{http_code}" \
    https://repo.maven.apache.org/maven2/com/microsoft/playwright/playwright/<version>/playwright-<version>.pom
```

`200` means the artifact is published; `404` means it is not yet available. Do **not** rely on the Maven Search API (`search.maven.org/solrsearch`) for this check — its index lags behind the repo and will miss recent releases. The direct URL is authoritative.

`gradle.properties`' `test.playwright.version` and the workflow's `npx playwright@<version>` invocation must be **pinned to the same version in the same commit**. Never update one without updating the other.

The original decision and its rationale are in `docs/ADR/adr-0004-github-actions-version-policy.md`.

## Success-assertion tautology pattern

`ResultAlert` emits the same `data-testid="<entity>-result"` regardless of state (success / danger / warning) because the alert region is one element whose class flips between `alert-success` and `alert-danger`. Waiting on the testId alone therefore also passes on failure — a tautology that has been shipped and caught in review.

The correct pattern AND-s the success class onto the `data-testid` selector:

```groovy
page.locator('[data-testid="organization-result"].alert-success').waitFor(
    new Locator.WaitForOptions().setTimeout(15_000)
)
```

Never write `page.locator('[data-testid="organization-result"]').waitFor(...)` as a post-condition for a "create succeeded" assertion. If the server returns an error, the alert still appears, the wait still resolves, and the test goes green on a regression.

## `<option>` elements need `ATTACHED` state, not `visible`

When waiting for an `<option>` inside a collapsed `<select>`, Playwright's default `visible` state treats it as hidden and the wait times out even though the element exists in the DOM. Use `WaitForSelectorState.ATTACHED`:

```groovy
page.locator("#vocabularyId option[value=\"${id}\"]").waitFor(
    new Locator.WaitForOptions()
        .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
        .setTimeout(15_000)
)
```

This applies to every cascading dropdown verification (category/vocabulary/thread pickers).

## `:has-text()` is substring; `:text-is()` is exact

`.nav-link:has-text("categories")` matches BOTH `categories` AND `mb-categories` tabs and triggers a Playwright strict-mode violation. When an entity label is a substring of another label, always use `:text-is()`:

```groovy
page.locator('.nav-link:text-is("categories")').click()
```

## Headless Delivery `?search=` goes through Elasticsearch

`/o/headless-delivery/v1.0/.../message-board-sections?search=...` hits the Elasticsearch index, which has ingestion latency. Tests running immediately after creation will get 0 results. For post-condition verification, drop `?search=` and fetch the full list with `?pageSize=100`, then filter client-side on `title` / `name`:

```groovy
def response = headlessGet(
    "/o/headless-delivery/v1.0/sites/${siteId}/message-board-sections?pageSize=100"
)
def matching = response.items.findAll { it.title?.startsWith(BASE_NAME) }
```

The `message-board-sections` listing endpoint is DB-backed (not ES-backed), so there is no indexing lag.

## Headless API vs Java API names for the same Message Boards entities

The IDs match between layers, so you can create with one API and verify/delete with the other.

| Java API     | Headless API             |
|--------------|--------------------------|
| `MBCategory` | `message-board-section`  |
| `MBThread`   | `message-board-thread`   |
| `MBMessage`  | `message-board-message`  |

## Workflow JSON workspace E2E

When testing the workflow JSON workspace, prefer stable `data-testid` locators over visible copy or proxy controls.

### What to assert

- The workspace opens after selecting `Workflow JSON`.
- The editor starts blank unless the user explicitly loads a sample.
- Clicking the in-editor sample loader changes the textarea value.
- `validate` / `plan` / `execute` should be asserted through the response path, not only through button clicks.
- Legacy entity forms still need a small regression check in the same spec so the new workspace does not break the old shell.

### Why this pattern

- Text labels change more often than test ids.
- A preloaded sample can make the sample-loader appear to work while actually doing nothing.
- Waiting on `response` or a result panel is safer than waiting on button presence alone.

### Selector guidance

- Use stable ids like `workflow-json-textarea` and `workflow-json-sample-load`.
- Avoid introducing proxy selectors in the shell when the real control already exists in the workspace.
- If a click is supposed to change the editor value, assert the value change directly before waiting on the backend response.
