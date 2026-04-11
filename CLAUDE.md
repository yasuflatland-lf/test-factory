# liferay-dummy-factory

Liferay Portal CE Workspace: MVCPortlet + React portlet with Testcontainers integration tests.

## Commands

```bash
./gradlew :modules:liferay-dummy-factory:build           # Full module build
./gradlew :modules:liferay-dummy-factory:test            # Unit tests + JaCoCo coverage report
./gradlew :integration-test:integrationTest                # Integration tests (requires Docker)
# Single spec:
./gradlew :integration-test:integrationTest --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

## Rules

`.claude/rules/` contains detailed guidelines (auto-loaded by Claude Code):
- `architecture.md` — Module structure, data flow, components
- `code-conventions.md` — Java, Groovy, JavaScript coding style
- `testing.md` — Integration test setup, frameworks, how to run

Keep this CLAUDE.md under 35 lines. Put detailed rules in `.claude/rules/` files instead.
