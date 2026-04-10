# Liferay Dummy Factory -- Architecture

## 1. Project Structure

```
liferay-dummy-factory/
  settings.gradle            # Liferay Workspace plugin + includes integration-test
  gradle.properties          # liferay.workspace.product=portal-7.4-ga132
  build.gradle
  modules/
    liferay-dummy-factory/ # Single OSGi bundle (portlet + web)
      bnd.bnd
      src/main/java/         # Java: portlet, resource commands, services, constants
      src/main/resources/
        META-INF/resources/
          js/               # React frontend components
  integration-test/          # Separate Gradle subproject at the repo root
    src/test/groovy/         # Spock/Groovy tests (Testcontainers + Playwright)
```

## 2. Portlet Module

**Single-JAR design** -- MVCPortlet, MVCResourceCommand, and React frontend live in one bundle (`liferay.dummy.factory`). The portlet and React UI ship in a single JAR.

**MVCPortlet + PanelApp** -- The portlet (`com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet`) is registered in the Control Panel under Configuration. The portlet uses `javax.portlet` namespace (Portlet API 3.0). The PanelApp's `@Reference` uses a target filter of `javax.portlet.name=...`. The view JSP renders the React component via the `<react:component>` tag.

**MVCResourceCommands** -- Entity creation is handled by per-entity resource commands: `/ldf/org` (Organization), `/ldf/user` (User), `/ldf/role` (Role), `/ldf/site` (Site). `/ldf/data` (`DataListResourceCommand`) serves dropdown data for organizations, roles, user-groups, site-roles, org-roles, sites, and site-templates.

**Value Objects** -- `BatchSpec` (Java record) encapsulates batch-creation parameters (count + baseName) with constructor validation, replacing scattered primitive validation. `RoleType` and `SiteMembershipType` are type-safe enums mapping frontend string values to Liferay integer constants, preventing silent fallback on invalid input. All resource commands construct these value objects from JSON input before passing them to Creator services.

**DataListProvider SPI** -- Dropdown data sources (organizations, roles, user-groups, sites, site-templates, etc.) are served by `DataListProvider` implementations discovered dynamically via OSGi `@Reference(cardinality=MULTIPLE, policy=DYNAMIC)`. New data types are added by creating a `@Component(service=DataListProvider.class)` class in `service/datalist/` — no changes to `DataListResourceCommand` needed.

**React frontend** -- React components live under `META-INF/resources/js/`. The view JSP passes server-side data (such as resource URLs) to the React component as props. The frontend uses `credentials: 'include'` so Liferay session cookies are sent automatically.

## 3. Integration Test Module

The `integration-test` subproject is included via `settings.gradle` at the repository root. It is intentionally **not** placed under `modules/` to avoid the Liferay Workspace Gradle plugin applying BundleSupport tasks to it.

Key components:

- **LiferayContainer** (`LiferayContainer.groovy`) -- A Testcontainers `GenericContainer` subclass wrapping `liferay/portal:7.4.3.132-ga132`. Singleton pattern (`getInstance()`) ensures one container per test run. Exposes HTTP (8080) and GogoShell (11311) ports. Waits for the Catalina startup log message (up to 8 minutes). Pre-configures setup wizard, terms-of-use, and reminder query to be disabled.
- **JAR deployment** -- `deployJar(Path)` copies the built module JAR into `/opt/liferay/deploy/` inside the container.
- **GogoShell verification** -- Tests connect to the mapped GogoShell port (11311) to verify bundle state.
- **Playwright UI tests** -- Browser-based tests hit the container's mapped HTTP port to exercise the portlet through the real UI.

## 4. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Single JAR** | MVCPortlet, MVCResourceCommand, and React frontend are bundled in one self-contained JAR, simplifying deployment. |
| **integration-test at repo root** | The Liferay Workspace plugin auto-applies to everything under `modules/`. Placing the test project at the root and including it explicitly in `settings.gradle` keeps it as a plain Gradle project with no Liferay plugin interference. |
| **`liferay/portal` CE image for tests** | Uses the free Community Edition Docker image (`liferay/portal:7.4.3.132-ga132`) so integration tests run without a DXP license. The workspace targets `portal-7.4-ga132` (Portal CE) for development. |
| **Container reuse (`withReuse(true)`)** | Liferay startup is slow (~8 min). Reusing the container across test runs drastically shortens feedback loops during development. |
| **Singleton container** | `getInstance()` with `synchronized` guarantees all test classes share a single Liferay instance, preventing resource waste and port conflicts. |
| **`release.portal.api` instead of `release.dxp.api`** | `release.dxp.api:default` resolves to 2026.q1.2 which provides `jakarta.portlet` (Portlet API 4.0). This is incompatible with the CE 7.4 GA132 Docker image, which uses `javax.portlet` (Portlet API 3.0). Using `release.portal.api` ensures the compile-time API matches the runtime. |
| **`actionResourceURLs` map** | The JSP generates per-entity action URLs as a map keyed by `mvc.command.name`, enabling the single React app to dispatch to multiple MVCResourceCommands. New entity types require adding a `<portlet:resourceURL>` entry in `view.jsp`. |
| **DataListProvider SPI** | Dropdown data sources are pluggable via an OSGi service interface with dynamic references, avoiding a growing switch statement in DataListResourceCommand. |
