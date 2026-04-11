# JaCoCo Step 2: Coverage for the Containerized Liferay JVM

## Current state (Step 1)

Step 1 instruments only the host-JVM unit tests under `modules/liferay-dummy-factory/src/test/java` with JaCoCo. The report is generated automatically after `test` via `finalizedBy`.

- HTML: `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- XML:  `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

Integration tests run against a containerized Liferay JVM via Testcontainers. None of the code executed inside that container is measured today, so coverage numbers reflect only the JUnit 5 host tests.

## Goal

Measure the portlet bundle's code as it is exercised by the integration-test suite, and merge that data with the host-JVM unit test data into a single consolidated report.

## Approach

### 1. Inject the JaCoCo agent into the Liferay JVM

Mount the JaCoCo agent jar and an empty exec file into the container, then extend `CATALINA_OPTS` so Catalina starts with the agent attached:

```
-javaagent:/opt/jacoco/jacocoagent.jar=destfile=/opt/jacoco/jacoco-it.exec,output=file,append=false,dumponexit=true
```

- `destfile` points at a path inside the container's writable volume.
- `output=file` writes directly to disk at JVM exit (no TCP server needed for a simple one-shot run).
- `dumponexit=true` ensures the file is flushed when the container is stopped gracefully.

### 2. Container wiring

Extend `LiferayContainer` to:

1. Resolve the agent jar from the Gradle classpath (`org.jacoco:org.jacoco.agent:<version>:runtime`).
2. Copy the agent into the container before start (`withCopyFileToContainer`).
3. Append the `-javaagent:...` entry to `CATALINA_OPTS` via an environment variable.
4. On stop, `copyFileFromContainer(/opt/jacoco/jacoco-it.exec, <host-path>)` to export the raw data.

Because `withReuse(false)` is mandated by `.claude/rules/testing.md`, every run produces a fresh exec file — no stale data to worry about.

### 3. Gradle integration

- New task `jacocoIntegrationReport` in `integration-test/build.gradle` that consumes the exported exec file and the portlet bundle's classes (`modules/liferay-dummy-factory/build/classes/java/main`) plus sources.
- Hook it via `finalizedBy` on `integrationTest` so the report is generated automatically on every run.

### 4. Merge unit + integration

- New task `jacocoMergeReport` (either in the root project or in a dedicated `coverage` subproject) that:
  1. Uses the JaCoCo `merge` task to combine `test.exec` (host unit tests) with `jacoco-it.exec` (container integration tests) into `jacoco-merged.exec`.
  2. Generates a single merged HTML + XML report from the merged exec file.
- Document the merged report path in `.claude/rules/testing.md` once the task is wired up.

## Open questions

- Agent version: align with the JaCoCo version already on the Gradle classpath to avoid format mismatches.
- Classloader scope: Liferay's OSGi wiring means the bundle classes are loaded by a bundle classloader, not the system classloader. Confirm that the agent still instruments them (it should, because the agent hooks `ClassFileTransformer` at the JVM level, not the classloader level).
- CI caching: the exec file must NOT be cached across runs. Gradle task outputs are per-run, which is the correct behavior.

## Out of scope for Step 2

- Coverage gates / thresholds in CI.
- Per-package or per-class exclusions beyond generated code.
- SonarQube / Codecov upload pipelines.

These belong to a later Step 3 once the merged report is trusted.
