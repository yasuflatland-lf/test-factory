# Code Conventions

## Java (Liferay Portal CE 7.4)

- Use **tab indentation** (not spaces).
- Prefer `@Reference` injection over static `*Util` classes (e.g., `TransactionInvoker` over `TransactionInvokerUtil`, `RoleLocalService` over `RoleLocalServiceUtil`). If a Liferay service is available as an OSGi service, inject it via `@Reference` for testability and consistency.
- Prefix private fields and methods with underscore: `_privateField`, `_doSomething(...)`.
- `@Component` annotations use array-style `property = { ... }` with one property per line, each as a quoted string. The `service` attribute goes on its own line after the closing brace.
- Use **javax.portlet** imports (`javax.portlet.Portlet`, `javax.portlet.version=3.0`), not `jakarta.portlet`. This project targets `release.portal.api:7.4.3.132` (Portal CE 7.4 GA132), which uses Portlet API 3.0. The `jakarta.portlet` namespace is for DXP 2024+ only and must NOT be used with `release.portal.api` / CE 7.4 Docker images.
- Organize imports in this order: `com.liferay.*`, third-party, `javax.*` / `java.*`, `org.*`. Separate each group with a blank line.
- Method parameters that span multiple lines use Liferay's continuation-indent style: second line indented two extra tabs, `throws` clause indented one extra tab.
- In `init.jsp`, always include both `<liferay-theme:defineObjects />` and `<portlet:defineObjects />`. The `<portlet:defineObjects />` tag is required for portlet implicit objects (`renderRequest`, `renderResponse`, etc.) to be available in JSPs.
- **Creator pattern (`TransactionInvokerUtil` + `throws Throwable`)** -- All `*Creator` classes in `service/` wrap each per-entity call in `TransactionInvokerUtil.invoke(_transactionConfig, () -> { ... })` with `REQUIRED` propagation and `Exception.class` rollback. The method signature declares `throws Throwable` because `TransactionInvokerUtil.invoke` itself throws `Throwable`. The corresponding `*ResourceCommand` catches `Throwable` (not just `Exception`) in `doServeResource` and routes it to `ResourceCommandUtil.setErrorResponse`. Rationale: a mid-loop failure in one entity must not roll back previously-created entities; each per-entity transaction commits independently. Reference implementation: `VocabularyCreator.java`, `OrganizationCreator.java`.
- **`DataListProvider` -- request-aware overload** -- `DataListProvider` exposes two `getOptions` overloads: a 2-arg `(companyId, type)` and a 3-arg `(companyId, type, HttpServletRequest)`. The default 3-arg implementation delegates to the 2-arg one, **dropping the request**. If your provider needs request parameters (e.g., `groupId` for site-scoped data), you MUST override the 3-arg overload directly -- overriding only the 2-arg overload silently loses request context. The no-request overload should return an empty `JSONArray` when the request-aware overload is the real implementation. Reference: `VocabulariesDataListProvider.java`, `MBCategoriesDataListProvider.java`.

## Groovy (Integration Tests)

- Spock spec method names use the descriptive string form: `def 'description of behavior'()`.
- Use `given:` / `when:` / `then:` / `expect:` blocks. Prefer `expect:` for single-expression assertions.
- Groovy idioms: safe-navigation `?.`, `withCloseable { ... }` for auto-closing resources, GString interpolation `"${variable}"`.
- All test classes live under package `com.liferay.support.tools.it.*` (e.g., `com.liferay.support.tools.it.spec`, `com.liferay.support.tools.it.container`, `com.liferay.support.tools.it.util`).
- Use `@Shared` for fields shared across feature methods. Use `@Stepwise` when test ordering matters.
- Numeric literals use underscores for readability: `30_000`, `10_000`.

## JavaScript / React

- **No direct React import** -- the project uses the new JSX transform, so `import React from 'react'` is omitted. Import only the hooks you need: `import {useState} from 'react'`.
- Use **Clay CSS** utility classes for all styling (`container-fluid`, `container-fluid-max-xl`, `sheet`, `sheet-lg`, `sheet-header`, `sheet-section`, `sheet-footer`, `form-group`, `form-control`, `btn btn-primary`, `alert alert-success`, `alert alert-danger`).
- Localization: always use `Liferay.Language.get('key-name')` for user-visible strings. Never hard-code display text.
- CSRF protection: include `'x-csrf-token': Liferay.authToken` in the headers of fetch requests to Liferay API endpoints when required.
- API calls use `portlet:resourceURL` with `credentials: 'include'`. GET requests pass parameters as URL query strings; POST requests use `application/x-www-form-urlencoded` body with JSON data in a `data` parameter.
- Build tooling is `@liferay/npm-scripts` -- do not add webpack/babel config manually.

## General

- No unnecessary comments. Code should be self-explanatory.
- No unused imports.
- Prefer simplicity. Avoid over-engineering or speculative abstractions.
- After any code change, verify that ALL related documentation is consistent and up to date. This includes: `CLAUDE.md`, `README.md`, `.claude/rules/` files, `docs/ADR/` files, and version references in `gradle.properties`. Naming, paths, versions, and descriptions must match the actual code.
