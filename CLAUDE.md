# liferay-dummy-factory

Liferay Portal CE Workspace: MVCPortlet + React portlet + Spock integration tests against `liferay/portal:7.4.3.132-ga132`.

## Routing — read the matching L2 file for the task you're starting

- New feature, refactor, or general code change → `.claude/rules/writing-code.md`
- Writing or running tests → `.claude/rules/testing.md`
- Bug investigation or test failure analysis → `.claude/rules/debugging.md`
- PR review or code-quality check → `.claude/rules/code-review.md`

## Where to find concrete details and history

- Concrete commands, selectors, version pins, API constraints → `docs/details/` (read on demand)
- Past architectural decisions and their rationale → `docs/ADR/` (read on demand)
- Workflow API specifics live in `docs/details/workflow-api.md`; it is the source of truth for `site.create`, `organization.create`, and the taxonomy-only startup fallback.

## Core contracts — break these and things go silently wrong

1. **Input boundary policy** — reject user input at the boundary; sanitize external-generated data. Never mix the two strategies.
2. **Single source of truth** — every fact, rule, or contract lives in exactly one file. Other files link to it.
3. **Creator pattern** — batch `*Creator` classes wrap per-entity work in `TransactionInvokerUtil.invoke` + `throws Throwable`. Detail in `.claude/rules/writing-code.md`.
4. **Batch response contract** — Creators return `{success, count, requested, skipped, error?, items}` with `success := created == requested` (strict). `error` MUST be set whenever `success == false`.
5. **JSONWS-first verification** — test post-conditions through `/api/jsonws/...`, not Playwright UI navigation. Detail in `.claude/rules/testing.md`.
6. **`javax.portlet` only** — `jakarta.portlet` does not work on CE 7.4 GA132. See `docs/ADR/adr-0002-portlet-api-javax-namespace.md`.
7. **`data-testid` is mechanically named** — `${entityKey}-${kebab(field)}-${typeSuffix}`. Do not invent ids; follow the contract in `.claude/rules/writing-code.md`.
8. **One package manager per repo** — `yarn.lock` only. Never coexist with `package-lock.json`.

## Common Gradle commands

```bash
./gradlew :modules:liferay-dummy-factory:jar           # Build the bundle JAR
./gradlew :modules:liferay-dummy-factory:test          # Host-JVM unit tests + JaCoCo
./gradlew :integration-test:integrationTest             # Spock + Testcontainers (Docker)
```

- DXP 2026 migration work — read `.claude/instructions/dxp-2026-references.md` first.
