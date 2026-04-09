# Code Conventions

## Java (Liferay DXP 2024+)

- Use **tab indentation** (not spaces).
- Prefix private fields and methods with underscore: `_privateField`, `_calculate(...)`.
- `@Component` annotations use array-style `property = { ... }` with one property per line, each as a quoted string. The `service` attribute goes on its own line after the closing brace.
- Use **jakarta.portlet** imports (`jakarta.portlet.Portlet`, `jakarta.portlet.version=4.0`), not the legacy `javax.portlet` namespace. This applies to all portlet-related code on DXP 2024+ / CE 7.4 GA120+.
- Organize imports in this order: `com.liferay.*`, third-party, `jakarta.*` / `java.*`, `org.*`. Separate each group with a blank line.
- Service Builder implementation: only edit `*Impl.java` files (e.g., `CalcEntryLocalServiceImpl`). Never hand-edit generated base classes, model classes, or persistence classes.
- Service Builder `*Impl` classes extend the generated `*BaseImpl` and are annotated with `@Component(property = "model.class.name=...", service = AopService.class)`.
- Method parameters that span multiple lines use Liferay's continuation-indent style: second line indented two extra tabs, `throws` clause indented one extra tab.
- Use `counterLocalService.increment()` for new primary keys and `*Persistence.create(id)` / `*Persistence.update(entity)` for CRUD.

## Groovy (Integration Tests)

- Spock spec method names use the descriptive string form: `def 'description of behavior'()`.
- Use `given:` / `when:` / `then:` / `expect:` blocks. Prefer `expect:` for single-expression assertions.
- Groovy idioms: safe-navigation `?.`, `withCloseable { ... }` for auto-closing resources, GString interpolation `"${variable}"`.
- All test classes live under package `com.liferay.test.factory.it.*` (e.g., `com.liferay.test.factory.it.spec`, `com.liferay.test.factory.it.container`, `com.liferay.test.factory.it.util`).
- Use `@Shared` for fields shared across feature methods. Use `@Stepwise` when test ordering matters.
- Numeric literals use underscores for readability: `30_000`, `10_000`.

## JavaScript / React

- **No direct React import** -- the project uses the new JSX transform, so `import React from 'react'` is omitted. Import only the hooks you need: `import {useState} from 'react'`.
- Use **Clay CSS** utility classes for all styling (`container-fluid`, `container-fluid-max-xl`, `sheet`, `sheet-lg`, `sheet-header`, `sheet-section`, `sheet-footer`, `form-group`, `form-control`, `btn btn-primary`, `alert alert-success`, `alert alert-danger`).
- Localization: always use `Liferay.Language.get('key-name')` for user-visible strings. Never hard-code display text.
- CSRF protection: include `'x-csrf-token': Liferay.authToken` in the headers of every fetch request to Liferay JSONWS endpoints.
- API calls go to `/api/jsonws/` endpoints with `credentials: 'include'` and `method: 'POST'` using `FormData`.
- Build tooling is `@liferay/npm-scripts` -- do not add webpack/babel config manually.

## General

- No unnecessary comments. Code should be self-explanatory.
- No unused imports.
- Prefer simplicity. Avoid over-engineering or speculative abstractions.
