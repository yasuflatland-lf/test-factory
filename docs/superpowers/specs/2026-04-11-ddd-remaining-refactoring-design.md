# DDD Remaining Refactoring Design

## Context

After introducing `BatchSpec`, `RoleType`, `SiteMembershipType`, `ApiResponse<T>` DU, and `DataListProvider` SPI, four DDD experts (増田亨, 松岡幸一郎, 杉本啓, t_wada) identified two remaining high-impact, low-effort improvements. 9 other candidates were rejected as YAGNI for a test data generation tool.

## Scope

Two independent tasks, parallelizable with zero file overlap.

---

## Task A: OrganizationCreator Duplicate Handling (Bug Fix)

**Problem:** `OrganizationCreator` is the only Creator (of 4) that does not catch duplicate entity exceptions. When a user creates organizations with names that already exist, the entire batch fails with a generic error. The other 3 Creators (`UserCreator`, `RoleCreator`, `SiteCreator`) gracefully skip duplicates and continue.

**Exception:** `com.liferay.portal.kernel.exception.DuplicateOrganizationException` — thrown by `OrganizationLocalService.addOrganization()` when an organization with the same name already exists within the company. Unique constraint is on `(companyId, name)`.

**Changes:**

### A1. `OrganizationCreator.java`
- Add `catch (DuplicateOrganizationException)` inside the creation loop, matching the pattern in `SiteCreator` (catch + log.warn + continue)
- Track skipped count for the response
- Import `com.liferay.portal.kernel.exception.DuplicateOrganizationException`

### A2. `OrganizationFunctionalSpec.groovy`
- Add a test case: create organizations, then create again with the same names — verify success with 0 created (all skipped), not an error

**Files:**
- `modules/.../service/OrganizationCreator.java`
- `integration-test/.../spec/OrganizationFunctionalSpec.groovy`

---

## Task B: entities.ts Field Definition Helpers (DRY)

**Problem:** `count` and `baseName` field definitions with identical validators are copy-pasted across 4 entity configs in `entities.ts`. Changing validation rules (e.g., adding a max count) requires editing 4 places.

**Changes:**

### B1. `entities.ts`
- Create `createCountField(label: string): FieldDefinition` helper
- Create `createBaseNameField(label: string): FieldDefinition` helper
- Replace 4 inline `count` definitions and 4 inline `baseName` definitions with helper calls
- Helpers are local to `entities.ts` (not exported)

**Files:**
- `modules/.../js/config/entities.ts`

---

## Parallel Execution

```
Group A (Backend)          Group B (Frontend)
  A1: OrganizationCreator    B1: entities.ts helpers
  A2: Integration test       
                             
  [No dependency]            [No dependency]
```

## Verification

```bash
# Task A
./gradlew :modules:liferay-dummy-factory:compileJava
./gradlew :integration-test:integrationTest --tests "*.OrganizationFunctionalSpec"

# Task B
cd modules/liferay-dummy-factory && npx jest
```

## What We Are NOT Doing (and Why)

| Rejected | Reason (YAGNI) |
|----------|----------------|
| UserCreator/SiteCreator parameter objects | Single call site, no behavioral value |
| Creator return type POJO | 8 files changed, conversion overhead for no consumer benefit |
| Common Creator interface | No polymorphic usage exists |
| Form value type safety | Data-driven forms vs static types incompatibility |
| Creator unit tests | Liferay mock cost high, integration tests cover |
| Contract tests | Integration tests are de facto contract tests |
