package com.liferay.support.tools.workflow.adapter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standardized batch-oriented result contract shared by workflow steps.
 */
public record WorkflowStepResult(
	int requested, int count, int skipped, boolean success, String error,
	List<Map<String, Object>> items) {

	public WorkflowStepResult {
		if (requested < 0) {
			throw new IllegalArgumentException("requested must be >= 0");
		}

		if (count < 0) {
			throw new IllegalArgumentException("count must be >= 0");
		}

		if (skipped < 0) {
			throw new IllegalArgumentException("skipped must be >= 0");
		}

		items = Collections.unmodifiableList(
			new ArrayList<>(Objects.requireNonNull(items, "items")));

		if (count != items.size()) {
			throw new IllegalArgumentException(
				"count must match items size");
		}
	}

}
