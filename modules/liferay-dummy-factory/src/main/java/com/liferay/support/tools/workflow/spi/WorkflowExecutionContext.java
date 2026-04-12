package com.liferay.support.tools.workflow.spi;

/**
 * Minimal execution context for workflow step adapters.
 */
public record WorkflowExecutionContext(long userId) {

	public WorkflowExecutionContext {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
	}

}
