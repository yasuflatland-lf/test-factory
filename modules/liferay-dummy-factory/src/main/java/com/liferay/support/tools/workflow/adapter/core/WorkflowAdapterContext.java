package com.liferay.support.tools.workflow.adapter.core;

/**
 * Minimal execution context that adapters need from the workflow runtime.
 */
public record WorkflowAdapterContext(long userId, long companyId) {
}
