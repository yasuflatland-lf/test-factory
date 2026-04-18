# Testing

L2 layer for test design, execution strategy, and verification. Read this when writing or running tests. Concrete commands, Playwright selectors, Vitest gotchas, and Gradle wiring details live in `docs/details/` and are linked inline.

## Framework versions

| Dependency      | Version          |
|-----------------|------------------|
| Spock           | 2.4-groovy-5.0   |
| Groovy          | 5.0.4            |
| Testcontainers  | 2.0.4            |
| Playwright      | 1.59.0           |

## Container setup

- Docker image: `liferay/dxp:2026.q1.3-lts` (DXP 2026).
- Singleton pattern: `LiferayContainer.getInstance()` starts one container per test run and shares it across all specs. Never create a second container instance inside a single run.
- Startup timeout: **8 minutes** (log-based wait strategy matching Catalina startup message).
- Exposed ports: **8080** (HTTP) and **11311** (GoGo Shell). Access via `liferay.httpPort` / `liferay.gogoPort` (mapped ports).
- Environment variables disable the setup wizard, terms-of-use prompt, and reminder queries so tests run unattended.
- **Container reuse is disabled** (`withReuse(false)`). Every Gradle run starts a fresh container so state from a previous run (created users, roles, sites, password changes) cannot leak across runs and mask regressions. Do not flip this to `true` for local speedups — state drift will cause false passes.

## Verification strategy: prefer JSONWS

- **Prefer Liferay JSONWS (`/api/jsonws/...`) over Playwright/UI navigation for verifying test outcomes.** JSONWS calls are faster, deterministic, and do not depend on Control Panel rendering or portlet UI state.
- Use Playwright when the assertion is about DOM/rendering, client-side validation, or navigation flows. For database-state assertions ("did the entity actually get created / updated / deleted?"), query JSONWS.
- Authenticate JSONWS calls with Basic Auth using the default admin credentials (`test@liferay.com` / `test`).
- See `BaseLiferaySpec` for `jsonwsGet` / `jsonwsPost` helpers and any `*FunctionalSpec` under `integration-test/.../spec/` for usage.

### JSON-WS exposure: only remote `*Service`, minus blacklist

- Liferay exposes remote `*Service` classes via `/api/jsonws/`, NOT `*LocalService`. If a method only exists on `*LocalService`, it cannot be called from a test.
- Some remote services are blacklisted via `portal.properties` `json.service.invalid.class.names`. `CompanyServiceUtil` is one such entry — every JSON-WS path under `/api/jsonws/company/*` returns HTTP 403 regardless of method or parameter format.
- Before writing cleanup or verification code for a new entity type, check both: (a) is there a remote `*Service` class with the method I need, and (b) is that class blacklisted? Catalogue of DXP 2026 API constraints: `docs/details/api-liferay-dxp2026.md`.

## Deploy verification

The bundle deploy / activation flow (`liferay.deployJar`, GoGo `lb`, `BaseLiferaySpec.ensureBundleActive()`, polling cadence) lives in `docs/details/testing-gradle.md`. Read it when wiring a new test that depends on bundle activation.

## Playwright (browser tests) — principles

- Runs **headless Chromium** via `PlaywrightLifecycle`.
- Login: `test@liferay.com` / `test`.
- Navigate by direct portlet URL (`p_p_id=...&p_p_lifecycle=0`), not by clicking through menus.
- Set explicit timeouts on every wait. Close `PlaywrightLifecycle` in `cleanupSpec()` with safe-navigation: `pw?.close()`.
- Selector strategy: `getByRole` → `aria-label` (i18n-stable only) → `data-testid`. Never select by visible text — `Liferay.Language.get(...)` values change per locale.

Concrete selector patterns, the `<option>` `ATTACHED` state rule, the `:has-text` vs `:text-is` distinction, the Java vs Node version skew, and Headless Delivery `?search=` Elasticsearch lag all live in `docs/details/testing-playwright.md`.

## Playwright success-assertion pattern

`ResultAlert` emits the same `data-testid="<entity>-result"` regardless of state — the alert region is one element whose class flips between `alert-success` and `alert-danger`. **Waiting on the testId alone passes on failure too.**

The correct pattern AND-s the success class onto the testId selector:

```groovy
page.locator('[data-testid="organization-result"].alert-success').waitFor(
    new Locator.WaitForOptions().setTimeout(15_000)
)
```

Never write `page.locator('[data-testid="organization-result"]').waitFor(...)` as the post-condition for a "create succeeded" assertion. If the server returns an error, the alert still appears, the wait still resolves, and the test goes green on a regression.

## Liferay.Language fallback gotcha (test side)

`Liferay.Language.get('missing-key')` returns the **key string itself** when not present in `Language.properties`. There is no warning, no console error — the raw kebab-case key is rendered to the user.

**Test impact**: a Playwright assertion like `:has-text("execution-completed-successfully")` passes even when the key is missing from `Language.properties`, because the DOM literally contains the key string. Both regressions go green: deleting a key, and never defining one in the first place.

**Authoring rule**: assert on the **resolved English phrase** from `Language.properties` (e.g. `"Execution completed successfully."`), never on the key identifier. If your assertion string contains hyphens and matches the key name, it's a code smell — look up the actual value.

## Vitest i18n fallback guard

`test/setup.ts` stubs `Liferay.Language.get` as `languageMap.get(key) ?? key`. If a key is removed from `Language.properties` but a test only asserts `expect(text).toBe('Create User')`, the test will keep passing by echoing the key back as its own value. Any unit test that asserts on a localized string MUST pair the positive assertion with a guard that rejects the fallback:

```ts
const text = Liferay.Language.get('create-user');
expect(text).not.toBe('create-user');
expect(text.length).toBeGreaterThan(0);
```

This guarantees the key actually resolved through `languageMap`. Silently deleting the key from `Language.properties` will then fail the test instead of passing through the identity fallback.

## Vitest unit test patterns

### `Language.properties` auto-load in `test/setup.ts`

- `modules/liferay-dummy-factory/test/setup.ts` reads `src/main/resources/content/Language.properties` **synchronously** with `fs.readFileSync` at module load time and parses it into a `Map<string, string>`. The global `Liferay.Language.get` stub then returns `languageMap.get(key) ?? key`. Unit tests see the real resolved values without any build step or per-spec mock wiring.
- The sync read is intentional: Vitest's `setupFiles` run before any test module, and an async load would require `beforeAll` plumbing in every spec. Sync I/O at setup time is fine — it runs once per worker, not per test.
- Comment-only lines (`#`) and blank lines are skipped; `key=value` is split on the **first** `=` so values containing `=` survive. Do not "improve" the parser to use `split('=')` — it will truncate values.
- The loader and the i18n fallback guard (above) are two halves of the same contract. Pair every localized-string assertion with the guard.

### `Mock<T>` + minimal-shape pattern

When a component under test calls a custom hook (`useFormState`, `useProgress`, etc.), cast the imported hook with Vitest's `Mock<T>` and return a **minimal object** coerced via `as unknown as ReturnType<typeof X>`:

```ts
import {vi, type Mock} from 'vitest';

const mockedUseFormState = useFormState as unknown as Mock<typeof useFormState>;
mockedUseFormState.mockReturnValue({
    formData: {count: 1, baseName: 'Test'},
    handleChange: vi.fn(),
    // ...only the fields the component actually reads
} as unknown as ReturnType<typeof useFormState>);
```

Do NOT replicate the hook's full return shape in the test — that couples the test to every field on the hook and makes adding a new field a multi-spec churn. The `as unknown as ReturnType<typeof X>` escape hatch is the intended pattern. The Vitest 2.x `Mock<T>` generic-argument pitfall (porting from Jest) is documented in `docs/details/ui-vitest-gotchas.md` (section B).

### Helper extraction stays in-file

Test helpers (render wrappers, fixture builders, mock factories) MUST stay inside the spec file that uses them. Do NOT create shared utility files under `test/js/utils/` or similar. If two specs need the same helper, copy it — the duplication is cheaper than the import graph and the coupling it creates.

The only exception is `test/setup.ts`, which is loaded globally by Vitest and is not a helper file in the usual sense.

## Test design: deterministic locks and branch coverage

Authoring rules that emerged from the `ScreenNameSanitizer` work. Not framework config — test-authoring discipline.

- **Pair RNG-based assertions with deterministic pattern locks.** When a test exercises a code path that consumes random or faker-generated data, the assertion that "the output looks right" only fails probabilistically on regression. The deterministic lock is a paired regex assertion on the normalized output:

	```groovy
	and: 'all returned screen names match Liferay-legal characters'
	(response.users as List).every {
	    (it.screenName as String) ==~ /^[a-z0-9._-]+$/
	}
	```

	**Every test that depends on RNG should also assert a deterministic pattern that a regression would break 100% of the time.** The deterministic lock is the actual regression guard; the RNG assertion only proves the path is exercised.

- **Pure-utility unit tests land before integration-test coverage.** A JUnit 5 host-JVM test for a pure function (`ScreenNameSanitizer.sanitize`, `BatchNaming.resolve`, `RoleType.fromString`) runs in ~8 seconds and catches character-class regressions without ever starting a Liferay container. Integration tests should lock down **integration-specific** behavior (that a branch exists, that JSON flows end-to-end, that Liferay actually accepts the sanitized output), not the correctness of the pure logic inside. When adding new utility classes under `src/main/java/.../utils/`, a matching `*Test.java` under `src/test/java` is mandatory.

- **Branching production changes require tests on both branches.** If a production change adds a new `if (fakerEnable)` / `else` split and the sanitize/validate logic differs between branches, integration tests must exercise **both** paths. An integration test that only sends `fakerEnable=true` does not cover the non-faker path's new `baseName` rejection logic, and vice versa. Add at least one dirty-input feature method (e.g. `rejects non-faker baseName that contains invalid characters` with `baseName: "O'Brien"`) next to the happy-path method.

- **Response-shape assertions should lock the contract, not the current values.** When a Creator's response shape is part of the contract (`success`, `count`, `requested`, `skipped`, `error?`), tests should assert **presence and type** of each field on both success and failure paths, not just the values seen in the happy case. A regression that silently drops the `skipped` field is otherwise undetectable. Add a one-line `response.containsKey('skipped')` check next to the value assertions.

- **Wire format renames require string-literal sweeps, not just dotted-access sweeps.** When a backend response field is renamed (e.g. `"users"` → `"items"`), grepping for `response.users` misses references through other variable names (`apiJson.users`, `apiResponseBody.contains('"users"')`, `body?.contains('"users"')`). The sweep must include the **string literal** form of the old field name as well:

	```bash
	# Dotted access (catches response.users, apiJson.users, etc.)
	grep -rn '\.users\b' integration-test/src/test/groovy/

	# String literal (catches contains("users"), put("users"), etc.)
	grep -rn '"users"' integration-test/src/test/groovy/
	```

	This is not hypothetical — the `"users"` → `"items"` rename in this project missed `UserFunctionalSpec` (`apiResponseBody.contains('"users"')`) and `UserRoleAssignmentSpec` (`apiJson.users`) because the initial sweep only covered `response.users`.

The Vitest migration gotcha catalogue (Mock typing, RTL cleanup, `vi.mock` hoisting, React double-resolution, ESM `setup.ts`) lives in `docs/details/ui-vitest-gotchas.md`. Read it when bumping the JS test toolchain.

## Gradle execution and the Incremental Build Trap

The full Gradle wiring (`integrationTest` task graph, JaCoCo coverage, deploy verification, the package.json input gap) lives in `docs/details/testing-gradle.md`. The single most important fact: `:integration-test:integrationTest` does not declare `package.json` as an input, so frontend-toolchain regressions can replay a cached green build. For any change touching the frontend toolchain, always run with `clean`:

```bash
./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
./gradlew :integration-test:integrationTest
```

Tell real runs from cached replays by the elapsed time on the `BUILD SUCCESSFUL in Xs` line. Single-digit seconds means nothing actually ran.

## Cleanup

### Acceptable to skip cleanup when the deletion API is unreachable

- `withReuse(false)` means a fresh Liferay container every test run. State from one run cannot leak into the next.
- If an entity has no working deletion path (no REST API, no JSON-WS exposure, no helpful `*LocalService` method), it is acceptable to skip `cleanupSpec` deletion entirely — just leave a one-line comment stating why. Example from `CompanyFunctionalSpec.groovy`: `// CompanyService is blacklisted from JSON-WS; container is disposable, so no explicit cleanup is needed.`
- Do NOT introduce best-effort cleanup code that logs warnings and swallows errors — it produces noise in test output and implies a bug. Either fix the cleanup or drop it with a comment.

## Adding new tests

1. Create a new Groovy class under `integration-test/src/test/groovy/com/liferay/support/tools/it/spec/`.
2. **Extend `BaseLiferaySpec`** — this gives you the shared `liferay` container instance and `ensureBundleActive()`.
3. Call `ensureBundleActive()` in `setupSpec()` (or in the first test) to guarantee the bundle is deployed and active before your tests run.
4. Use `@Stepwise` if your tests must execute in declaration order (e.g., login then interact).
5. For browser tests, instantiate `PlaywrightLifecycle` as a `@Shared` field in `setupSpec()` and close it in `cleanupSpec()`.
)`.
