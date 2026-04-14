# Workflow API

This document describes the workflow JAX-RS API mounted at `/o/ldf-workflow`.

## Endpoints

- `POST /o/ldf-workflow/plan`
- `POST /o/ldf-workflow/execute`
- `GET /o/ldf-workflow/schema`
- `GET /o/ldf-workflow/functions`

## Contract Shape

- `plan` and `execute` reuse the existing workflow DTOs in `com.liferay.support.tools.workflow.dto`.
- Requests use `schemaVersion`, `workflowId`, `input`, and ordered `steps`.
- Each step executes from top to bottom.
- Step parameters use exactly one of:
  - `value`: literal JSON value
  - `from`: reference to workflow input or an earlier step result
- The API is registered with OSGi JAX-RS whiteboard string properties:
  - `osgi.jaxrs.application.base=/o/ldf-workflow`
  - `osgi.jaxrs.name=ldf-workflow`
  - `osgi.jaxrs.application.select=(osgi.jaxrs.name=ldf-workflow)`

## Operational Notes

- `site.create` creates a normal top-level site unless a parent site or other site-specific options are passed explicitly.
- `organization.create` creates an organization and only creates an organization site when the `site` parameter is set to `true`.
- `vocabulary.create` and `category.create` have a narrow startup fallback in the JAX-RS resource: if the OSGi adapter registration is temporarily missing, the resource registers those two operations directly from the creator services so `/functions` and `/plan` stay usable.
- The fallback is intentionally scoped to taxonomy operations only. Other workflow functions still rely on the normal OSGi adapter registration path.

## Reference Syntax

- `input.<property>[.<nestedProperty>|[index]...]`
- `steps.<stepId>.<property>[.<nestedProperty>|[index]...]`

Examples:
- `input.pageTitle`
- `steps.createSite.items[0].groupId`
- `steps.createSite.data.slug`

## Execution Model

- `plan` validates request shape plus workflow semantics:
  - unknown operations
  - duplicate step ids
  - duplicate parameter names within a step
  - invalid `from` expressions
  - references to later or missing steps
  - missing required parameters defined by each registered workflow function
- `execute` runs the same validation first.
- If validation fails, `execute` returns `errors` and leaves `execution` as `null`.
- If validation succeeds, steps run sequentially and each successful step result is available to later `from` references.
- Only `FAIL_FAST` is supported right now, so execution stops at the first failing step.

## Discovery Endpoints

- `GET /schema`
  - returns the generic JSON Schema for workflow requests
  - includes the currently available `operation` enum values
  - documents `from` reference syntax
- `GET /functions`
  - returns the currently registered workflow functions
  - includes per-function parameter metadata, required flags, descriptions, and defaults when available

## Execute Response

- `execution`
  - `null` when request validation fails
  - otherwise a `WorkflowExecutionResult`
- `errors`
  - empty on successful validation
  - populated with structured validation errors when the request is invalid

## Example

```json
{
  "schemaVersion": "1.0",
  "workflowId": "sample-site-pipeline",
  "input": {
    "pageTitle": "Welcome"
  },
  "steps": [
    {
      "id": "createSite",
      "operation": "site.create",
      "idempotencyKey": "site-1",
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "Demo Site"}
      ]
    },
    {
      "id": "createLayout",
      "operation": "layout.create",
      "idempotencyKey": "layout-1",
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "Home"},
        {"name": "groupId", "from": "steps.createSite.items[0].groupId"},
        {"name": "type", "value": "portlet"}
      ]
    }
  ]
}
```

## Current Limitations

- Only sequential execution is supported.
- Only `FAIL_FAST` is supported for `onError.policy`.
- The JSON Schema is generic by step shape; per-operation required parameters are exposed through `/functions` and runtime validation rather than embedded as operation-specific JSON Schema branches.
