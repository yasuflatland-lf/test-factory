# test-factory

Liferay Portal CE Workspace: calculator portlet (Service Builder + React) with Testcontainers integration tests.

## Commands

```bash
./gradlew :modules:test-factory-calculator:buildService   # Service Builder codegen
./gradlew :modules:test-factory-calculator:build           # Full module build
./gradlew :integration-test:integrationTest                # Integration tests (requires Docker)
# Single spec:
./gradlew :integration-test:integrationTest --tests "com.liferay.test.factory.it.spec.DeploymentSpec"
```

## Rules

`.claude/rules/` contains detailed guidelines (auto-loaded by Claude Code):
- `architecture.md` — Module structure, data flow, components
- `code-conventions.md` — Java, Groovy, JavaScript coding style
- `testing.md` — Integration test setup, frameworks, how to run
