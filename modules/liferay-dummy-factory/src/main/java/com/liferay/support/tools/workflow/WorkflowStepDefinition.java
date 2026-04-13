package com.liferay.support.tools.workflow;

import java.util.List;

public record WorkflowStepDefinition(
	String id,
	String operation,
	String idempotencyKey,
	List<WorkflowParameter> parameters,
	WorkflowErrorPolicy onErrorPolicy) {

	public WorkflowStepDefinition {
		if ((id == null) || id.isBlank()) {
			throw new IllegalArgumentException("step id is required");
		}

		if ((operation == null) || operation.isBlank()) {
			throw new IllegalArgumentException("operation is required");
		}

		if ((idempotencyKey == null) || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("idempotencyKey is required");
		}

		if (parameters == null) {
			throw new IllegalArgumentException("parameters is required");
		}

		parameters = List.copyOf(parameters);

		if (onErrorPolicy == null) {
			throw new IllegalArgumentException("onErrorPolicy is required");
		}
	}

}
