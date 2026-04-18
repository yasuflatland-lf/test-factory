# Writing Code

L2 layer for new features, refactoring, and general code modifications. For tests use `.claude/rules/testing.md`, for PR review use `.claude/rules/code-review.md`, for bug investigation use `.claude/rules/debugging.md`.

## Project Structure

```
liferay-dummy-factory/
  modules/liferay-dummy-factory/   # Single OSGi bundle (portlet + web + React)
    src/main/java/                  # MVCPortlet, ResourceCommands, Creator services
    src/main/resources/META-INF/resources/js/   # React frontend
  integration-test/                 # Spock + Testcontainers (repo root, NOT under modules/)
```

Detailed DXP 2026 API constraints: `docs/details/api-liferay-dxp2026.md`. Workspace frontend traps: `docs/details/workspace-frontend-traps.md`. Read on demand.

## Portlet Module

- **Single-JAR design** — MVCPortlet, MVCResourceCommand, and React frontend ship together in one bundle (`liferay.dummy.factory`).
- **MVCPortlet + PanelApp** — Registered in Control Panel > Configuration. Uses `jakarta.portlet` namespace (Portlet API 4.0). DXP 2026 requires `jakarta.portlet.*` imports (see `docs/ADR/adr-0008-dxp-2026-migration.md`).
- **MVCResourceCommands** — Per-entity resource commands handle creation: `/ldf/blog`, `/ldf/company`, `/ldf/org`, `/ldf/user`, `/ldf/role`, `/ldf/site`, `/ldf/page`, `/ldf/wcm`, `/ldf/doc` (+ `/ldf/doc/upload`), `/ldf/vocabulary`, `/ldf/category`, `/ldf/mb-category`, `/ldf/mb-thread`, `/ldf/mb-reply`. `/ldf/data` (`DataListResourceCommand`) serves dropdown data; `/ldf/progress` (`ProgressResourceCommand`) reports batch progress.
- **Value Objects** — `BatchSpec` (Java record) encapsulates `count + baseName` with constructor validation. `RoleType` and `SiteMembershipType` are type-safe enums mapping frontend strings to Liferay constants. Resource commands construct value objects from JSON before passing to Creators.
- **DataListProvider SPI** — Dropdown sources are `DataListProvider` implementations discovered via OSGi `@Reference(cardinality=MULTIPLE, policy=DYNAMIC)`. Add a new type by creating `@Component(service=DataListProvider.class)` under `service/datalist/` — no changes to `DataListResourceCommand` needed.
- **`bnd.bnd` must exclude `javax.servlet`**: DXP 2026 does not export `javax.servlet` or
  `javax.servlet.http` from the OSGi runtime. Always include this line in
  `modules/liferay-dummy-factory/bnd.bnd`:
  ```
  Import-Package: !javax.servlet,!javax.servlet.http,*
  ```
  Without it, the bundle will show as UNSATISFIED at activation time.

## Java Conventions

- **Tab indentation** (no spaces).
- Prefer `@Reference` injection over `*Util` static classes (`TransactionInvoker` over `TransactionInvokerUtil`, `RoleLocalService` over `RoleLocalServiceUtil`) for testability.
- Private fields/methods get an underscore prefix: `_privateField`, `_doSomething(...)`.
- `@Component` annotations use array-style `property = { ... }` with one quoted string per line. The `service` attribute lives on its own line after the closing brace.
- Use `jakarta.portlet` imports. DXP 2026 requires `jakarta.portlet.*` imports and
	`jakarta.portlet.version=4.0` in `@Component` property arrays. Do NOT use `javax.portlet.*`.
	JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0` — the JCP namespace is what
	DXP 2026 advertises via `Provide-Capability`. Switching to `jakarta.tags.portlet` in JSPs
	causes bundle resolution failure. See `docs/ADR/adr-0008-dxp-2026-migration.md`.
- Import order: `com.liferay.*` → third-party → `javax.*`/`java.*` → `org.*`. Blank line between groups.
- Multi-line method parameters use Liferay's continuation indent: second line +2 tabs, `throws` clause +1 tab.
- `init.jsp` must include both `<liferay-theme:defineObjects />` and `<portlet:defineObjects />`.
- **`init.jsp` taglib URI must stay JCP**: Use `http://xmlns.jcp.org/portlet_3_0` as the
  portlet taglib URI in all JSPs. DXP 2026's `Provide-Capability` only advertises this URI.
  Switching to `jakarta.tags.portlet` causes bundle resolution failure. A comment in
  `init.jsp` marks this as intentional — do not change it.

## Creator pattern (`TransactionInvokerUtil` + `throws Throwable`)

Every `*Creator` under `service/` wraps each per-entity call in `TransactionInvokerUtil.invoke(_transactionConfig, () -> { ... })`:

- Propagation `REQUIRED`, rollback on `Exception.class`.
- Method signature declares `throws Throwable` because `TransactionInvokerUtil.invoke` itself throws `Throwable`.
- Corresponding `*ResourceCommand` catches `Throwable` (NOT `Exception`) in `doServeResource` and routes to `ResourceCommandUtil.setErrorResponse`.

**Why**: a mid-loop failure must not roll back already-created entities; each per-entity transaction commits independently.

Reference: `VocabularyCreator.java`, `OrganizationCreator.java`.

## DXP 2026 API call shapes

### `GroupLocalService.addGroup` — 18-argument signature

DXP 2026 adds `externalReferenceCode` as the first argument and `typeSettings`
before `serviceContext`. Pass `null` for both new parameters when no custom value is needed:

```java
_groupLocalService.addGroup(
	null,                // externalReferenceCode (auto-generated)
	userId, parentGroupId, className, classPK,
	liveGroupId, nameMap, descriptionMap, type,
	manualMembership, membershipRestriction, friendlyURL,
	site, inheritContent, active,
	null,                // typeSettings (defaults)
	serviceContext);
```

Full API constraints: `docs/details/api-liferay-dxp2026.md`.

### Early validation outside the transaction boundary

When a Creator validates input before looping (e.g. a regex check on `baseName`), throw the validation exception **outside** any `TransactionInvokerUtil.invoke(...)` call. No transaction has started, so no rollback is needed. The `throws Throwable` contract plus the resource command's `catch (Throwable)` automatically routes the exception through `ResourceCommandUtil.setErrorResponse` → `{success: false, error: "..."}`. No extra plumbing required.

Do NOT wrap validation in `invoke(...)` "for consistency" with per-entity calls — it costs a meaningless null-rollback and hides the contract that input validation happens at the boundary.

- **`BatchTransaction.run(...)` for per-entity transactions** — Every Creator's per-entity call must be wrapped in `com.liferay.support.tools.utils.BatchTransaction.run(() -> ...)` rather than inlining `TransactionInvokerUtil.invoke(_transactionConfig, ...)` with a local `_transactionConfig` field. The helper centralizes the transaction configuration (`Propagation.REQUIRED`, rollback on `Exception.class`) so that a future cross-cutting change (retry, metrics, MDC) can be applied in one place instead of 12+ Creators. Do not re-introduce local `TransactionConfig` fields in new Creators.
- **Response key for batch items is always `items`** — Every Creator's response JSON uses the key `items` for the array of created entities, regardless of entity type. Do NOT invent entity-specific keys like `"users"`, `"roles"`, `"organizations"`. Exception: `WebContentCreator` uses a per-site multi-group response shape (`ok`/`totalRequested`/`totalCreated`/`perSite`) that cannot be flattened without information loss; this is the only documented exception. The exemption exists because `WebContentCreator` batches across multiple sites (`groupIds`), producing per-site results (`perSite: [{groupId, siteName, created, failed, error}, ...]`). Flattening into a single `items` array would lose per-site failure attribution — the UI could no longer display "Site A succeeded, Site B failed". The full response contract is `{success, requested, count, skipped, items, error?}` — see the "Batch Creator response contract" bullet for invariants.
- **Typed `*BatchSpec` records absorb per-batch parameters** — When a Creator's method signature grows past ~5 parameters, extract the per-batch configuration into a Java `record` named `*BatchSpec` (e.g. `UserBatchSpec`, `WebContentBatchSpec`). The record composes the shared `BatchSpec(count, baseName)` via a `BatchSpec batch` field rather than inlining count/baseName. The record's compact constructor normalizes nullable/empty inputs to their documented defaults so the `*ResourceCommand` does not need to repeat defensive null checks. Reference implementations: `UserBatchSpec.java`, `WebContentBatchSpec.java`. Creators that currently have ≤5 parameters (e.g. `OrganizationCreator`, `RoleCreator`) should continue to use raw parameters + `BatchSpec` — do not introduce a dedicated spec where it is not needed.

### Contract enforcement: Pattern A vs Pattern B

The responsibility for assembling the unified response contract (`{success, requested, count, skipped, items, error?}`) depends on the Creator's return type:

- **Pattern A** (`JSONObject` return) — the Creator itself builds and returns the contract-conformant JSON. The ResourceCommand passes it through unchanged.
- **Pattern B** (`List<LiferayModel>` return) — the Creator returns a typed list; the ResourceCommand is responsible for wrapping it into the contract shape.

Do not assume which pattern a Creator follows from its name or from documentation — verify by reading the return type of its `create` method. `CompanyCreator` was historically misclassified as Pattern A in planning documents but actually returns `List<Company>`.

## Input-boundary policy: reject user input, sanitize external data

- **User-supplied strings** (e.g. `baseName` from a portlet form) must be validated and rejected at the resource-command or Creator boundary. **Never silently rewrite them** — a user typing `山田` who silently gets users named `1, 2, 3` has no way to discover the substitution.
- **External-generated data** (Datafaker, RNGs, third-party APIs) must instead be **sanitized** because the caller can't control the content.
- Mixing the strategies produces silent UX bugs (sanitizing user input) or probabilistic test failures (validating faker output).

Reference: `com.liferay.support.tools.utils.ScreenNameSanitizer` is the sanitization side; `UserCreator`'s pre-loop regex check is the rejection side.

## Batch Creator response contract

Every batch-producing `*Creator` returns:

```
{success, count, requested, skipped, error?, items}
```

- `success` is **strict**: `created == requested`, NOT `created > 0`. Requesting 10 and producing 3 is a **failure**, not a partial success.
- Whenever `success == false`, `error` MUST be set. Enforce this with an unconditional `else` branch inside the `if (!success)` block so future `continue` paths cannot silently drop the error.
- `requested` and `skipped` are load-bearing for partial-failure diagnostics — keep them even if the current frontend doesn't consume them.

### Pattern B limitation: `skipped` is structurally untrackable

Pattern B Creators return `List<LiferayModel>` and do not currently use catch-and-continue loops, so `skipped: 0` in the ResourceCommand is accurate today. However, if a future change adds a catch-and-continue path to a Pattern B Creator (e.g. catching `DuplicateException` and skipping), the ResourceCommand has no way to know — it only sees `list.size()` vs `requested` and cannot distinguish "skipped" from "errored".

If catch-and-continue is needed in a Pattern B Creator, convert it to Pattern A (Creator assembles its own `JSONObject` with `skipped` tracking) or change the return type to a result object that carries `count + skipped + items`.

## `ResourceCommandUtil.setErrorResponse` writes `error` (not `errorMessage`)

The helper writes the failure message to the JSON field `error`. New resource commands must use this helper. Do NOT invent alternate field names like `errorMessage`, `message`, `reason`, or `detail` — the frontend `parseResponse` does not read them.

## Groovy (Integration Tests)

- Spock spec method names use the descriptive form: `def 'description of behavior'()`.
- Use `given:` / `when:` / `then:` / `expect:` blocks. Prefer `expect:` for single-expression assertions.
- Idioms: safe-navigation `?.`, `withCloseable { ... }`, GString `"${variable}"`.
- All test classes live under `com.liferay.support.tools.it.*`.
- `@Shared` for fields shared across feature methods. `@Stepwise` when ordering matters.
- Underscores in numeric literals: `30_000`, `10_000`.

## JavaScript / React

- **No direct React import** — the new JSX transform is enabled, so `import React from 'react'` is omitted. Import only the hooks you need: `import {useState} from 'react'`.
- Use **Clay CSS** utility classes (`container-fluid`, `sheet`, `form-group`, `form-control`, `btn btn-primary`, `alert alert-success`, `alert alert-danger`, …).
- Localization: always use `Liferay.Language.get('key-name')`. Never hard-code display text.
- CSRF: include `'x-csrf-token': Liferay.authToken` in fetch headers when required by Liferay endpoints.
- API calls use `portlet:resourceURL` with `credentials: 'include'`. GET passes parameters as URL query strings; POST uses `application/x-www-form-urlencoded` with JSON in a `data` parameter.
- Build tooling is a custom esbuild script (`scripts/build.mjs`). `@liferay/npm-scripts` is retained for code formatting only (`format` / `checkFormat`). See `docs/ADR/adr-0003-custom-esbuild-over-npm-scripts.md`.

### `parseResponse` must check both `data.success === false` and `data.error`

`success` and `error` are **independent** fields in the backend response contract. Checking only `data.error` silently classifies `{success: false}` as success. Checking only `data.success` misses legacy responses that set only `error`. The correct form is `if (data.success === false || data.error)`.

A failure-classified response must pass the **full `data` payload** through to the caller (not just `{error, success: false}`) so partial-batch responses like `{success: false, count: 3, requested: 5, users: [...], error: "..."}` can be rendered without a second round-trip.

`ApiResponse<T>` failure variant carries optional data: `{success: false; data?: T; error: string}`.

The backend/frontend error field name is `error` — not `errorMessage`, `message`, or `reason`.

### Liferay.Language fallback gotcha

`Liferay.Language.get('missing-key')` returns the **key string itself** when not present in `Language.properties`. No warning, no console error — the raw kebab-case key is rendered to the user.

**Authoring rule**: when writing a Playwright assertion on localized text, assert on the **resolved English phrase** from `Language.properties` (e.g. `"Execution completed successfully."`), never on the key identifier. If your assertion string contains hyphens and matches the key name, it's a code smell — look up the actual value.

**Adding a key**: always add the entry to `Language.properties` in the same commit that introduces the `Liferay.Language.get('...')` call.

### Frontend i18n loading: JSP-injected ResourceBundle

**Why:** Custom ESM builds (esbuild/Vite via `scripts/build.mjs`) bypass Liferay's `LanguageUtil.process()` server-side JS replacement. Standard `@liferay/npm-bundler` portlets get `Liferay.Language.get('key')` calls rewritten to literal values at serve-time by `BuiltInJSModuleServlet`. Custom ESM bundles are served as static resources via the OSGi HTTP Whiteboard — no rewriting occurs, so `Liferay.Language._cache` starts empty and every `get()` call returns the raw key.

**What:** `view.jsp` injects the portlet's own ResourceBundle into `Liferay.Language._cache` before `<react:component>` renders. Use `portletConfig.getResourceBundle(locale)` — NOT `LanguageUtil.get(Locale, key)`, which only checks portal-global bundles and misses module-specific keys.

```jsp
<%
ResourceBundle resourceBundle = portletConfig.getResourceBundle(locale);
JSONObject languageKeys = JSONFactoryUtil.createJSONObject();
Enumeration<String> enumeration = resourceBundle.getKeys();
while (enumeration.hasMoreElements()) {
	String key = enumeration.nextElement();
	languageKeys.put(key, resourceBundle.getString(key));
}
%>
<script>
	Object.assign(Liferay.Language._cache, <%= languageKeys.toJSONString() %>);
</script>
```

### Playwright selector strategy

- Priority: `getByRole` → `aria-label` (i18n-stable only) → `data-testid`.
- **Never select by visible text** (`has-text`, `getByText`) — `Liferay.Language.get(...)` values change per locale.

### `data-testid` naming and generation contract

- **Naming**: kebab-case domain term + role (`organization-create-submit`, `user-count-input`, `role-type-select`). No UI-positional names (`btn1`) or BEM-style (`Form__submit`).
- **Placement scope**: only on elements Playwright actually interacts with (button, input, select, result/alert region, tab). Do not decorate links, icons, or purely visual elements.
- **Reusable components**: `FormField`, `DynamicSelect`, `ResultAlert` accept an optional `testId?: string` and emit `data-testid={testId}` only when provided.
- **`entityKey` derivation**: `EntityForm.tsx` computes `entityKey = config.entityType.toLowerCase().replace(/_/g, '-')`. So `ORG` → `org`, `MB_THREAD` → `mb-thread`, `USERS` → `users`. Playwright specs MUST use the derived value — do not invent synonyms like `organization-submit`.
- **`EntitySelector` tabs are NOT kebab-cased** — tabs emit `data-testid={`entity-selector-${entityType}`}` using the raw enum value (`entity-selector-MB_THREAD`). This is an intentional exception. Use upper-snake on the selector and kebab everywhere else.
- **Generation contract**: form ids are assembled mechanically as `${entityKey}-${kebab(field.name)}-${typeSuffix}`. `typeSuffix`: `text`/`number` → `input`, `select`/`multiselect` → `select`, `textarea` → `textarea`, `file` → `file`, `checkbox` → `toggle`. Submit is always `${entityKey}-submit`, result alert always `${entityKey}-result`. Adding `maxUsers: number` to `ORG` yields `org-max-users-input` predictably.

## DataListProvider — request-aware overload

`DataListProvider` exposes two `getOptions` overloads: 2-arg `(companyId, type)` and 3-arg `(companyId, type, HttpServletRequest)`. The default 3-arg implementation delegates to the 2-arg one and **drops the request**. If your provider needs request parameters (e.g. `groupId` for site-scoped data) you MUST override the 3-arg overload directly. Reference: `VocabulariesDataListProvider.java`, `MBCategoriesDataListProvider.java`.

## General

- No unnecessary comments. Code is self-explanatory.
- No unused imports.
- Prefer simplicity. No over-engineering or speculative abstractions.
- After any code change, verify all related documentation is consistent: `CLAUDE.md`, `README.md`, `.claude/rules/`, `docs/details/`, `docs/ADR/`, `gradle.properties`. Naming, paths, and version numbers must match the actual code.

## One package manager per repo

Never let `package-lock.json` and `yarn.lock` coexist. This project uses `yarn.lock`. Resolve a `yarn.lock` merge conflict by taking one side wholesale (`--ours` or `--theirs`) and running `yarn install` to let Yarn rewrite the file. Never hand-edit `yarn.lock`.

Dependency version pinning for the JS/React toolchain (vite/vitest/plugin-react/jsdom, `@types/react` lockstep, etc.) lives in `docs/details/dependency-policy.md`.
