# Gradle test execution — concrete details

L3 detail. Source of truth for Gradle task wiring, deploy verification, the incremental build trap, and JaCoCo coverage. Read on demand from `.claude/rules/testing.md` or `.claude/rules/debugging.md`.

## Running tests

```bash
# Build the module JAR first (required by integration tests)
./gradlew :modules:liferay-dummy-factory:jar

# Run integration tests (requires Docker)
./gradlew :integration-test:integrationTest

# Single spec
./gradlew :integration-test:integrationTest \
    --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

- The module build depends on `release.portal.api` (Portal CE), **not** `release.dxp.api`. Using the wrong dependency artifact will cause build failures or runtime class-loading issues against the CE Docker image. The original decision is recorded in `docs/ADR/adr-0002-portlet-api-javax-namespace.md`.
- The default `test` task is **disabled** (`enabled = false`). All integration tests run exclusively via the `integrationTest` task.
- The `integrationTest` task automatically depends on `:modules:liferay-dummy-factory:jar`, so a standalone `./gradlew :integration-test:integrationTest` will build the JAR first.
- JVM args: `-Xms4g -Xmx4g`.
- Test logging outputs `passed`, `skipped`, `failed`, `standardOut`, and `standardError`.

## Deploy verification

1. The module JAR is copied into the container at `/opt/liferay/deploy/` using `liferay.deployJar(path)`.
2. The JAR must be pre-built — running `./gradlew :modules:liferay-dummy-factory:jar` is a hard prerequisite.
3. Bundle activation is verified via GoGo Shell: `lb | grep dummy.factory` must show `Active` or `ACTIVE`.
4. The `ensureBundleActive()` method in `BaseLiferaySpec` polls GoGo Shell every 5 seconds for up to 5 minutes until the bundle is active. It is `synchronized` and runs only once per test suite.

## Gradle Incremental Build Trap

`:integration-test:integrationTest` does **not** declare `package.json` as an input. Changing a JavaScript dependency (e.g. bumping React, swapping Jest for Vitest) does not invalidate the `integrationTest` task, so Gradle marks it `UP-TO-DATE` and **replays the previous run's result** without executing anything. A regression introduced in the JS toolchain will appear as a green build.

For any change touching the frontend toolchain, the only trustworthy verification is:

```bash
./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
./gradlew :integration-test:integrationTest
```

Tell real runs from cached replays by the elapsed time on the `BUILD SUCCESSFUL in Xs` line. A real run is on the order of minutes (container startup + Playwright); a cached replay is single-digit seconds. If the build completes in single-digit seconds, assume the tests did not actually run and re-invoke with the `clean` tasks above.

## Coverage (JaCoCo)

Host-JVM unit tests under `modules/liferay-dummy-factory/src/test/java` are measured by JaCoCo. The report is generated automatically after `test` via `finalizedBy`.

```bash
./gradlew :modules:liferay-dummy-factory:test
```

Report locations:

- HTML: `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- XML:  `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

**Scope limitation**: only host-JVM unit tests (JUnit 5) are covered. Integration tests run against a containerized Liferay JVM (Testcontainers) and are **not** instrumented in this step. Coverage for the containerized JVM requires injecting the JaCoCo agent into Liferay's `CATALINA_OPTS` and is deferred to a later step.
