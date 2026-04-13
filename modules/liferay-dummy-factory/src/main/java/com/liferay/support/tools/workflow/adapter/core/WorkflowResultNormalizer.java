package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class WorkflowResultNormalizer {

	static WorkflowStepResult normalize(JSONObject jsonObject) {
		Objects.requireNonNull(jsonObject, "jsonObject");

		JSONArray itemsArray = _requireJSONArray(jsonObject, "items");
		int requested = _requireInt(jsonObject, "requested");
		int count = _requireInt(jsonObject, "count");
		int skipped = _requireInt(jsonObject, "skipped");
		boolean success = _requireBoolean(jsonObject, "success");
		String error = _optionalString(jsonObject, "error");

		List<Map<String, Object>> items = new ArrayList<>(itemsArray.length());

		for (int i = 0; i < itemsArray.length(); i++) {
			Object value = itemsArray.get(i);

			if (!(value instanceof JSONObject itemJSONObject)) {
				throw new IllegalArgumentException(
					"items[" + i + "] must be a JSONObject");
			}

			items.add(_toMap(itemJSONObject));
		}

		if (count != items.size()) {
			throw new IllegalArgumentException("count must match items size");
		}

		return new WorkflowStepResult(
			requested, count, skipped, success, error, items);
	}

	private static boolean _requireBoolean(
		JSONObject jsonObject, String key) {

		Object value = _requireValue(jsonObject, key);

		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}

		throw new IllegalArgumentException(key + " must be a boolean");
	}

	private static int _requireInt(JSONObject jsonObject, String key) {
		Object value = _requireValue(jsonObject, key);

		if (value instanceof Number number) {
			return number.intValue();
		}

		throw new IllegalArgumentException(key + " must be a number");
	}

	private static JSONArray _requireJSONArray(
		JSONObject jsonObject, String key) {

		Object value = _requireValue(jsonObject, key);

		if (value instanceof JSONArray jsonArray) {
			return jsonArray;
		}

		throw new IllegalArgumentException(key + " must be a JSONArray");
	}

	private static Object _requireValue(JSONObject jsonObject, String key) {
		if (!jsonObject.has(key)) {
			throw new IllegalArgumentException(key + " is required");
		}

		Object value = jsonObject.get(key);

		if (value == null) {
			throw new IllegalArgumentException(key + " is required");
		}

		return value;
	}

	private static String _optionalString(JSONObject jsonObject, String key) {
		if (!jsonObject.has(key)) {
			return null;
		}

		Object value = jsonObject.get(key);

		if (value == null) {
			return null;
		}

		if (value instanceof String string) {
			return string;
		}

		throw new IllegalArgumentException(key + " must be a string");
	}

	private static List<Object> _toList(JSONArray jsonArray) {
		List<Object> list = new ArrayList<>(jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(_normalizeValue(jsonArray.get(i)));
		}

		return list;
	}

	private static Map<String, Object> _toMap(JSONObject jsonObject) {
		Map<String, Object> map = new LinkedHashMap<>();

		for (String key : jsonObject.keySet()) {
			map.put(key, _normalizeValue(jsonObject.get(key)));
		}

		return map;
	}

	private static Object _normalizeValue(Object value) {
		if (value instanceof JSONObject jsonObject) {
			return _toMap(jsonObject);
		}

		if (value instanceof JSONArray jsonArray) {
			return _toList(jsonArray);
		}

		return value;
	}

	private WorkflowResultNormalizer() {
	}

}
