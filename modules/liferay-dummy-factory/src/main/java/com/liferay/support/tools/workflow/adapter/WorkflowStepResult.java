package com.liferay.support.tools.workflow.adapter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record WorkflowStepResult<T>(
	String operation, int requested, int count, int skipped, boolean success,
	List<T> items, String error) {

	public WorkflowStepResult {
		Objects.requireNonNull(operation, "operation is required");

		if (requested < 0) {
			throw new IllegalArgumentException(
				"requested must be greater than or equal to 0");
		}

		if (count < 0) {
			throw new IllegalArgumentException(
				"count must be greater than or equal to 0");
		}

		if (skipped < 0) {
			throw new IllegalArgumentException(
				"skipped must be greater than or equal to 0");
		}

		items = List.copyOf((items == null) ? List.of() : items);
	}

	public static <S, T> WorkflowStepResult<T> fromItems(
		String operation, int requested, List<S> sourceItems,
		Function<S, T> itemMapper, String noun) {

		List<T> items = ((sourceItems == null) ? List.<S>of() : sourceItems
		).stream(
		).map(
			itemMapper
		).toList();

		int count = items.size();
		int skipped = Math.max(0, requested - count);
		boolean success = (count == requested);
		String error = null;

		if (!success) {
			error =
				"Only " + count + " of " + requested + " " + noun +
					" were created.";
		}

		return new WorkflowStepResult<>(
			operation, requested, count, skipped, success, items, error);
	}

}
