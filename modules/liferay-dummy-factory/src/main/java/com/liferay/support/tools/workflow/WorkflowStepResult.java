package com.liferay.support.tools.workflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WorkflowStepResult(
	boolean success,
	int requested,
	int count,
	int skipped,
	String error,
	List<Map<String, Object>> items,
	Map<String, Object> data) {

	public WorkflowStepResult {
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

		if ((count + skipped) > requested) {
			throw new IllegalArgumentException(
				"count + skipped must be less than or equal to requested");
		}

		error = _normalizeError(error);

		if (success && (count != requested)) {
			throw new IllegalArgumentException(
				"success requires count to equal requested");
		}

		if (!success && (error == null)) {
			throw new IllegalArgumentException(
				"error is required when success is false");
		}

		items = _copyItems(items);
		data = Map.copyOf(new LinkedHashMap<>(_nullSafeMap(data)));
	}

	public Map<String, Object> asMap() {
		Map<String, Object> values = new LinkedHashMap<>();

		values.put("success", success);
		values.put("requested", requested);
		values.put("count", count);
		values.put("skipped", skipped);
		values.put("items", items);

		if (error != null) {
			values.put("error", error);
		}

		if (!data.isEmpty()) {
			values.put("data", data);
		}

		return Map.copyOf(values);
	}

	public static WorkflowStepResult failure(
		int requested, int count, int skipped, String error,
		List<Map<String, Object>> items, Map<String, Object> data) {

		return new WorkflowStepResult(
			false, requested, count, skipped, error, items, data);
	}

	public static WorkflowStepResult success(
		int requested, List<Map<String, Object>> items,
		Map<String, Object> data) {

		int count = _nullSafeList(items).size();

		return new WorkflowStepResult(
			true, requested, count, 0, null, items, data);
	}

	private static List<Map<String, Object>> _copyItems(
		List<Map<String, Object>> items) {

		List<Map<String, Object>> safeItems = new ArrayList<>();

		for (Map<String, Object> item : _nullSafeList(items)) {
			safeItems.add(Map.copyOf(new LinkedHashMap<>(_nullSafeMap(item))));
		}

		return List.copyOf(safeItems);
	}

	private static <T> List<T> _nullSafeList(List<T> values) {
		if (values == null) {
			return List.of();
		}

		return values;
	}

	private static Map<String, Object> _nullSafeMap(Map<String, Object> values) {
		if (values == null) {
			return Map.of();
		}

		return values;
	}

	private static String _normalizeError(String error) {
		if ((error == null) || error.isBlank()) {
			return null;
		}

		return error;
	}

}
