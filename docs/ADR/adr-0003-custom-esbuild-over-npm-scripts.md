# ADR-0003: Use Custom esbuild Pipeline Instead of @liferay/npm-scripts

## Status

Accepted

## Date

2026-04-12

## Context

The portlet's React frontend needs a JavaScript build pipeline that produces an ESM bundle compatible with Liferay CE 7.4 GA132's AMD module loader. Two options were evaluated:

1. **`@liferay/npm-scripts` (v51.x)** — Liferay's official Webpack-based build toolchain for frontend modules
2. **Custom esbuild script** (`scripts/build.mjs`) — a ~165-line build script using esbuild directly

### Why not `@liferay/npm-scripts`

| Concern | Detail |
|---------|--------|
| DXP-first assumptions | npm-scripts targets the portal monorepo and DXP release cadences. External workspace modules on CE 7.4 hit edge cases (dependency resolution, API version skew) |
| Build speed | npm-scripts wraps Webpack, which is significantly slower than esbuild for single-module builds |
| Unnecessary complexity | npm-scripts includes SASS processing, export bridge generation, TypeScript type-checking, and a full linker plugin for 200+ portal-global packages. This portlet needs none of those — it uses Clay CSS classes, has a single entry point, and only externalizes `react` and `react-dom` |
| Liferay's own direction | The portal monorepo has replaced npm-scripts with `@liferay/node-scripts` (esbuild-based). npm-scripts is the legacy path |
| Transparency | A 165-line build script is fully readable and debuggable. npm-scripts is a black-box pipeline with plugin chains that are difficult to trace when something breaks |

### What the custom build produces

The script outputs the same artifact structure that Liferay's module loader expects:

| Artifact | Path | Purpose |
|----------|------|---------|
| ESM bundle | `__liferay__/index.js` | esbuild output with react/react-dom externalized |
| AMD bridge | `index.js` | `Liferay.Loader.define()` wrapper that imports the ESM bundle |
| `manifest.json` | root | Declares `{esModule: true, useESM: true}` for the AMD loader |
| `package.json` | root | Module identity for Liferay's frontend registry |

## Decision

### 1. Use esbuild via `scripts/build.mjs` for production bundling

The build script handles four steps sequentially:
1. Bundle TSX/TS source into a single ESM file via esbuild
2. Generate an AMD bridge (`index.js`) that registers the module with `Liferay.Loader.define()`
3. Write `package.json` with module name and version
4. Write `manifest.json` with ESM flags

### 2. Retain `@liferay/npm-scripts` for formatting only

`package.json` scripts:
```json
{
  "build": "node scripts/build.mjs",
  "checkFormat": "liferay-npm-scripts format --check",
  "format": "liferay-npm-scripts format"
}
```

`@liferay/npm-scripts` remains a `devDependency` exclusively for its `format` command, which enforces Liferay's code style. It does not participate in the production build.

### 3. Externalize react and react-dom to Liferay's shared copies

A custom esbuild plugin (`liferayReactExternalsPlugin`) redirects `react` and `react-dom` imports to the portal's shared bundles at `../../frontend-js-react-web/__liferay__/exports/react.js`. This prevents React from being duplicated in the browser.

### 4. Handle frontend i18n via JSP ResourceBundle injection

Because the ESM bundle is served as a static resource (bypassing Liferay's `LanguageUtil.process()` server-side replacement), `view.jsp` injects language keys from `portletConfig.getResourceBundle(locale)` into `Liferay.Language._cache` before the React component renders. See the "Frontend i18n loading" section in `.claude/rules/writing-code.md`.

## Consequences

### Positive

- Build time is sub-second for incremental changes (esbuild), vs multi-second Webpack builds
- The build script is fully visible and debuggable — no hidden plugin chains
- Output format is identical to what Liferay's AMD loader expects
- Aligned with Liferay's own migration direction (npm-scripts → node-scripts/esbuild)

### Negative

- **No content-addressed filenames** — output is `__liferay__/index.js` (no hash). Browser caching requires cache-busting via query strings or short TTLs. Acceptable for a development/admin tool
- **No build-time language key extraction** — `Liferay.Language.get()` calls are left as-is in the bundle. i18n resolution happens at runtime via the JSP injection pattern, not at build time. This means all keys are loaded regardless of whether they're used in the current page
- **Manual externalization** — only `react` and `react-dom` are externalized. If the portlet imports other portal-global packages in the future, the plugin must be updated manually
- **No watch mode / HMR** — rebuild requires `./gradlew :modules:liferay-dummy-factory:jar` + container deploy. Acceptable for a tool with infrequent frontend changes
- **Maintenance burden** — the ~165-line build script must track changes to Liferay's AMD bridge format, manifest schema, or module URL conventions across portal upgrades

### Lessons Learned

- `LanguageUtil.get(Locale, key)` (used by the language servlet) only checks portal-global language bundles. Module-specific Language.properties keys are invisible to it. Use `portletConfig.getResourceBundle(locale)` for module-scoped keys
- The language servlet's `all.js` uses ESM `export default` — loading it via `<script>` (non-module) causes a SyntaxError
- `Liferay.Language._cache` starts empty in the browser. For ESM portlets, the cache must be explicitly populated before React renders

## References

- Custom build script: `modules/liferay-dummy-factory/scripts/build.mjs`
- AMD bridge format: compare with portal's `modules/frontend-sdk/node-scripts/util/amd/writeMainBridge.mjs`
- Language injection: `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/view.jsp` (lines 20-36)
- Frontend i18n architecture: `.claude/rules/writing-code.md` § "Frontend i18n loading: JSP-injected ResourceBundle"
- ADR-0002: Use javax.portlet (3.0) for the Portlet API (related CE 7.4 compatibility decision)
