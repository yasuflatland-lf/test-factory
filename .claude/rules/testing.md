# Testing

## Framework Versions

| Dependency      | Version          |
|-----------------|------------------|
| Spock           | 2.4-groovy-5.0   |
| Groovy          | 5.0.4            |
| Testcontainers  | 2.0.4            |
| Playwright      | 1.59.0           |

## Container Setup

- Docker image: `liferay/portal:7.4.3.132-ga132` (CE GA132).
- Singleton pattern: `LiferayContainer.getInstance()` starts one container per test run and shares it across all specs. Never create a second container instance inside a single run.
- Startup timeout: **8 minutes** (log-based wait strategy matching Catalina startup message).
- Exposed ports: **8080** (HTTP) and **11311** (GoGo Shell). Access via `liferay.httpPort` / `liferay.gogoPort` (mapped ports).
- Environment variables disable the setup wizard, terms-of-use prompt, and reminder queries so tests run unattended.
- **Container reuse is disabled** (`withReuse(false)`). Every Gradle run starts a fresh container so state from a previous run (created users, roles, sites, password changes) cannot leak across runs and mask regressions. Do not flip this to `true` for local speedups — state drift will cause false passes.

## Verification Strategy: Prefer JSONWS

- **Prefer Liferay JSONWS (`/api/jsonws/...`) over Playwright/UI navigation for verifying test outcomes.** JSONWS calls are faster, deterministic, and do not depend on Control Panel rendering or portlet UI state.
- Use Playwright when the assertion is about DOM/rendering, client-side validation, or navigation flows. For database-state assertions ("did the entity actually get created / updated / deleted?"), query JSONWS.
- Authenticate JSONWS calls with Basic Auth using the default admin credentials (`test@liferay.com` / `test`).
- See `BaseLiferaySpec` for `jsonwsGet` / `jsonwsPost` helpers, and any `*FunctionalSpec` under `integration-test/.../spec/` for usage.

### JSON-WS exposure: only remote *Service, minus blacklist

- Liferay exposes remote `*Service` classes via `/api/jsonws/`, NOT `*LocalService`. If a service method only exists on `*LocalService`, it cannot be called from a test.
- Some remote services are explicitly blacklisted via `portal.properties` `json.service.invalid.class.names`. `CompanyServiceUtil` is one such entry — every JSON-WS path to `/api/jsonws/company/*` returns HTTP 404 regardless of method or parameter format.
- Before writing cleanup/verification code for a new entity type, check both (a) is there a remote `*Service` class with the method I need, and (b) is that class blacklisted?

## Deploy Verification

1. The module JAR is copied into the container at `/opt/liferay/deploy/` using `liferay.deployJar(path)`.
2. The JAR must be pre-built: run `./gradlew :modules:liferay-dummy-factory:jar` before running tests.
3. Bundle activation is verified via GoGo Shell: `lb | grep dummy.factory` must show `Active` or `ACTIVE`.
4. The `ensureBundleActive()` method in `BaseLiferaySpec` polls GoGo Shell every 5 seconds for up to 5 minutes until the bundle is active. It is synchronized and runs only once per test suite.

## PortletTracker and jakarta.portlet Compatibility (CE GA132)

The PortletTracker in CE 7.4 GA132 tracks `javax.portlet.Portlet` services, **not** `jakarta.portlet.Portlet`. If the portlet's `@Component` declares `service = jakarta.portlet.Portlet.class`, the PortletTracker will not detect it and the corresponding `com.liferay.portal.kernel.model.Portlet` OSGi service will never be registered. This causes any PanelApp with a `@Reference` to that portlet model to stay in an **UNSATISFIED** state.

**Debugging steps:**

1. Run `scr:info <component-name>` in GoGo Shell and look for `UNSATISFIED REFERENCE` entries. If the portlet model reference is unsatisfied, the PortletTracker is not registering the portlet.
2. Run `headers <bundle-id>` in GoGo Shell and inspect the `Import-Package` header to verify which portlet API package the bundle actually imports (`javax.portlet` vs `jakarta.portlet`).
3. If the bundle imports `jakarta.portlet`, switch the portlet `@Component` to declare `service = javax.portlet.Portlet.class` (and use `javax.portlet` imports) so the PortletTracker can detect it on CE GA132.

## Playwright (Browser Tests)

- Runs **headless Chromium** via `PlaywrightLifecycle`.
- Login credentials: `test@liferay.com` / `test` (Liferay default admin).
- Navigate to portlets via **direct URL** with the portlet ID in the query string (`p_p_id=...&p_p_lifecycle=0`). Do not click through menus -- direct navigation is faster and more reliable.
- Use CSS selectors for locators (`#count`, `.alert-success`, `button.btn-primary`, `[type=submit]`).
- Set explicit timeouts on waits: `waitForURL(..., new Page.WaitForURLOptions().setTimeout(30_000))`, `waitFor(new Locator.WaitForOptions().setTimeout(15_000))`.
- Close the `PlaywrightLifecycle` instance in `cleanupSpec()` using safe-navigation: `pw?.close()`.

## Playwright Success Assertion Pattern

- **Always AND the success class onto the result `data-testid` selector.** `ResultAlert` emits the same `data-testid="<entity>-result"` regardless of state (success / danger / warning), because the alert region is a single element whose class flips between `alert-success` and `alert-danger`. Waiting on the testId alone therefore also passes on failure — a tautology that was actually shipped and caught in review.
- The correct pattern is to wait on the testId **plus** the success class together:

	```groovy
	page.locator('[data-testid="organization-result"].alert-success').waitFor(
	    new Locator.WaitForOptions().setTimeout(15_000)
	)
	```

	Never write `page.locator('[data-testid="organization-result"]').waitFor(...)` as a post-condition for a "create succeeded" assertion. If the server returns an error, the alert still appears, the wait still resolves, and the test turns green on a regression.

## Liferay.Language Fallback Gotcha

- `Liferay.Language.get('missing-key')` in a running Liferay portal returns **the key string itself** when the key is not present in `Language.properties`. There is no warning, no console error, and no visual marker — the raw kebab-case key is rendered directly to the end user.
- **Runtime impact:** a typo or a deleted key surfaces to users as `execution-completed-successfully` or `create-user` instead of the translated phrase. Since the string is non-empty and looks superficially like valid text, the bug slips through manual smoke tests.
- **Test impact:** Playwright assertions written as `page.locator(':has-text("execution-completed-successfully")')` will pass even when the key is missing from `Language.properties`, because the DOM literally contains that key string. A regression that deletes the key goes green. A regression that never defined the key in the first place goes green. This exact pattern was shipped and later caught in review.
- **Authoring rule:** when writing a Playwright assertion on localized text, assert on the **resolved** English phrase from `Language.properties` (e.g. `"Execution completed successfully."`), never on the key identifier. If the assertion string contains hyphens and matches the key name, that is a code smell — look up the actual value.
- **Adding a new key:** always add the entry to `Language.properties` in the same commit that introduces the `Liferay.Language.get('...')` call. See the Jest i18n Fallback Guard below for the unit-test side of the same problem.

## Jest i18n Fallback Guard

- `test/setup.ts` stubs `Liferay.Language.get` as `languageMap.get(key) ?? key`. If a key is removed from `Language.properties` but a test only asserts `expect(text).toBe('Create User')`, the test will keep passing by echoing the key back as its own value. Any unit test that asserts on a localized string MUST pair the positive assertion with a guard that rejects the fallback:

	```ts
	const text = Liferay.Language.get('create-user');
	expect(text).not.toBe('create-user');
	expect(text.length).toBeGreaterThan(0);
	```

	This guarantees that the key actually resolved through `languageMap`, so silently deleting the key from `Language.properties` will fail the test instead of passing through the identity fallback.

## Jest Unit Test Patterns

### Language.properties auto-load in `test/setup.ts`

- `modules/liferay-dummy-factory/test/setup.ts` reads `src/main/resources/content/Language.properties` **synchronously** with `fs.readFileSync` at module load time and parses it into a `Map<string, string>`. The global `Liferay.Language.get` stub then returns `languageMap.get(key) ?? key`. This means unit tests see the real resolved values without any build step or per-spec mock wiring.
- The sync read is intentional: Jest's global `setup.ts` runs before any test module, and an async load would require `beforeAll` plumbing in every spec. Sync I/O at setup time is fine — it runs once per worker, not per test.
- Comment-only lines (`#`) and blank lines are skipped; `key=value` is split on the **first** `=` so values containing `=` survive. Do not "improve" the parser to use `split('=')` — it will truncate values.
- Pair every localized-string assertion with the i18n fallback guard documented above in **Jest i18n Fallback Guard**. The loader and the guard are two halves of the same contract.

### `jest.MockedFunction` + minimal-shape pattern

- When a component under test calls a custom hook (`useFormState`, `useProgress`, etc.), cast the imported hook with `jest.MockedFunction<typeof X>` and return a **minimal object** coerced via `as unknown as ReturnType<typeof X>`. Example from `EntityForm.test.tsx`:

	```ts
	const mockedUseFormState = useFormState as jest.MockedFunction<typeof useFormState>;
	mockedUseFormState.mockReturnValue({
		formData: {count: 1, baseName: 'Test'},
		handleChange: jest.fn(),
		// ...only the fields the component actually reads
	} as unknown as ReturnType<typeof useFormState>);
	```

	Do NOT replicate the hook's full return shape in the test — that couples the test to every field on the hook and makes adding a new field a multi-spec churn. The `as unknown as ReturnType<typeof X>` escape hatch is the intended pattern.

### Helper extraction stays in-file

- Test helpers (render wrappers, fixture builders, mock factories) MUST stay inside the spec file that uses them. Do NOT create shared utility files under `test/js/utils/` or similar. If two specs need the same helper, copy it — the duplication is cheaper than the import graph and the coupling it creates.
- The only exception is `test/setup.ts`, which is loaded globally by Jest and is not a helper file in the usual sense.

## Playwright / Headless Gotchas

- **`<option>` elements need `ATTACHED` state, not `visible`.** When waiting for an option inside a collapsed `<select>`, Playwright's default `visible` state treats it as hidden and the wait times out even though the element exists in the DOM. Use `setState(WaitForSelectorState.ATTACHED)`:

	```groovy
	page.locator("#vocabularyId option[value=\"${id}\"]").waitFor(
	    new Locator.WaitForOptions()
	        .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
	        .setTimeout(15_000)
	)
	```

	This applies to every cascading dropdown verification (category/vocabulary/thread pickers).

- **`:has-text()` is substring, `:text-is()` is exact.** `.nav-link:has-text("categories")` matches BOTH "categories" AND "mb-categories" tabs and triggers a Playwright strict-mode violation. When an entity label is a substring of another label, always use `:text-is()`:

	```groovy
	page.locator('.nav-link:text-is("categories")').click()
	```

- **Headless Delivery `?search=` goes through Elasticsearch.** `/o/headless-delivery/v1.0/.../message-board-sections?search=...` hits the search index, which has ingestion latency. Tests running immediately after creation will get 0 results. For post-condition verification, drop `?search=` and fetch the full list with `?pageSize=100`, then filter client-side on `title` / `name`:

	```groovy
	def response = headlessGet("/o/headless-delivery/v1.0/sites/${siteId}/message-board-sections?pageSize=100")
	def matching = response.items.findAll { it.title?.startsWith(BASE_NAME) }
	```

	The `message-board-sections` listing endpoint is DB-backed, not ES-backed, so there is no indexing lag.

## Cleanup

### Acceptable to skip cleanup when the deletion API is unreachable

- `withReuse(false)` means a fresh Liferay container every test run. State from one run cannot leak into the next.
- If an entity has no working deletion path (no REST API, no JSON-WS exposure, no helpful `*LocalService` method), it is acceptable to skip `cleanupSpec` deletion entirely — just leave a one-line comment stating why. Example from `CompanyFunctionalSpec.groovy`: `// CompanyService is blacklisted from JSON-WS; container is disposable, so no explicit cleanup is needed.`
- Do NOT introduce best-effort cleanup code that logs warnings and swallows errors — it produces noise in test output and implies a bug. Either fix the cleanup or drop it with a comment.

- **Headless API and Java API use different names for the same Message Boards entities.** The `id` values are identical between the two layers, so you can create via one and verify/delete via the other.

	| Java API | Headless API |
	|----------|--------------|
	| `MBCategory` | `message-board-section` |
	| `MBThread` | `message-board-thread` |
	| `MBMessage` | `message-board-message` |

## Running Tests

```bash
# Build the module JAR first (required)
./gradlew :modules:liferay-dummy-factory:jar

# Run integration tests (requires Docker)
./gradlew :integration-test:integrationTest
```

- The module build depends on `release.portal.api` (Portal CE), **not** `release.dxp.api`. Using the wrong dependency artifact will cause build failures or runtime class-loading issues against the CE Docker image.
- The default `test` task is **disabled** (`enabled = false`). All integration tests run exclusively via the `integrationTest` task.
- The `integrationTest` task automatically depends on `:modules:liferay-dummy-factory:jar`, so a standalone `./gradlew :integration-test:integrationTest` will build the JAR first.
- JVM args: `-Xms4g -Xmx4g`.
- Test logging outputs `passed`, `skipped`, `failed`, `standardOut`, and `standardError`.

## Coverage (JaCoCo)

Host-JVM unit tests under `modules/liferay-dummy-factory/src/test/java` are measured by JaCoCo. The report is generated automatically after `test` via `finalizedBy`.

```bash
./gradlew :modules:liferay-dummy-factory:test
```

Report locations:
- HTML: `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- XML:  `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

**Scope limitation**: Only the host-JVM unit tests (JUnit 5) are covered. Integration tests run against a containerized Liferay JVM (Testcontainers) and are **not** instrumented in Step 1. Coverage for the containerized JVM requires injecting the JaCoCo agent into Liferay's `CATALINA_OPTS` and will be handled in a later step.

## Adding New Tests

1. Create a new Groovy class under `integration-test/src/test/groovy/com/liferay/support/tools/it/spec/`.
2. **Extend `BaseLiferaySpec`** -- this gives you the shared `liferay` container instance and `ensureBundleActive()`.
3. Call `ensureBundleActive()` in `setupSpec()` (or in the first test) to guarantee the bundle is deployed and active before your tests run.
4. Use `@Stepwise` if your tests must execute in declaration order (e.g., login then interact).
5. For browser tests, instantiate `PlaywrightLifecycle` as a `@Shared` field in `setupSpec()` and close it in `cleanupSpec()`.
