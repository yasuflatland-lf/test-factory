# Liferay Workspace frontend traps

L3 detail. Non-obvious pitfalls hit while wiring the React frontend and its unit-test stack into a Liferay Workspace module. Read on demand from `.claude/rules/writing-code.md`.

## J25: `@liferay/npm-scripts` bundles its own `@testing-library/react@14`

Even when the module's `package.json` pins `@testing-library/react@16.3.0` at the top level, npm emits an `incorrect peer dependency "react@^18.0.0"` warning because `@liferay/npm-scripts` brings in a transitive copy of v14 along with its own React 18 testing stack.

The warning is **harmless** and can be ignored — the top-level v16 wins for the module's own Vitest runs, and the bundled v14 copy is only used by `@liferay/npm-scripts` internals. Do not attempt to "resolve" the warning by downgrading the top-level pin or by mucking with `resolutions`/`overrides`; both make the actual test stack inconsistent with the React version the production bundle ships.

## J26: `esbuild` build path is independent of Vite/Vitest

`scripts/build.mjs` uses `esbuild` plus an AMD Loader bridge to produce the Liferay-compatible bundle, while unit tests run under Vitest. These two toolchains do **not** need to be unified — shipping a production build via esbuild while running tests via Vitest is a supported split.

Do NOT attempt to collapse them onto a single bundler just for consistency. The constraints are different:

- The production bundle must be AMD-compatible for Liferay's loader, which esbuild + the AMD bridge handles cleanly.
- The Vitest run only needs to evaluate ESM modules in jsdom; it never produces an output bundle.

Forcing both onto Vite for "consistency" requires re-implementing the AMD bridge inside Vite plugins, which is strictly more code and more failure modes than the split design.

## J27: `LanguageUtil.process()` replaces string-literal `Liferay.Language.get()` calls at serve-time

**Why:** Liferay's resource serving pipeline applies `LanguageUtil.process()` to JS files, which regex-replaces `Liferay.Language.get('string-literal')` with the resolved value. For module-specific keys not registered via `ResourceBundleLoader`, the key itself becomes the literal — silently breaking i18n. Variable-parameter calls (`Liferay.Language.get(variable)`) are not matched by the regex and survive to runtime.

**What:** Never hardcode module-specific i18n keys as string literals in component code. Pass keys via variables (e.g. `Liferay.Language.get(field.label)` instead of `Liferay.Language.get('upload-template-files')`). If string literals are unavoidable, register a `ResourceBundleLoader` component (`LDFResourceBundleLoader.java`) with `Provide-Capability: liferay.resource.bundle` in `bnd.bnd` to make the module's `Language.properties` globally visible to `LanguageUtil`.

## J28: New `MVCResourceCommand` endpoints require `view.jsp` registration

**Why:** The React frontend resolves resource URLs from `<portlet:resourceURL>` tags rendered in `view.jsp`. Without registration, `LdfResourceClient` (integration tests) and the frontend `postResource` function cannot find the endpoint URL, producing "Could not find resource URL" errors.

**What:** When adding a new `MVCResourceCommand` (e.g. `/ldf/blog`), add both entries to `view.jsp` in the same commit:
1. `<portlet:resourceURL id="/ldf/blog" var="blogResourceURL" />`
2. `actionResourceURLs.put("/ldf/blog", blogResourceURL)` in the HashMap initialization
