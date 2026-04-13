package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CreatorJsonNormalizer {

	public static WorkflowStepResult normalizeStandardResult(
		Object creatorResult) {

		JSONObject jsonObject = _requireJSONObject(creatorResult);

		boolean success = _requireBoolean(jsonObject, "success");
		long requested = _requireLong(jsonObject, "requested");
		long count = _requireLong(jsonObject, "count");
		long skipped = _requireLong(jsonObject, "skipped");
		JSONArray itemsArray = _requireJSONArray(jsonObject, "items");
		String error = _optionalString(jsonObject, "error");

		List<Map<String, Object>> items = _toList(itemsArray);

		if (count != items.size()) {
			throw new IllegalArgumentException("count must match items size");
		}

		return new WorkflowStepResult(
			success, requested, count, skipped, items, error);
	}

	public static WorkflowStepResult normalizeWebContentResult(
		Object creatorResult) {

		JSONObject jsonObject = _requireJSONObject(creatorResult);
		JSONArray perSite = _requireJSONArray(jsonObject, "perSite");
		List<Map<String, Object>> items = new ArrayList<>(perSite.length());
		long skipped = 0;

		for (int i = 0; i < perSite.length(); i++) {
			Object value = perSite.get(i);

			if (!(value instanceof JSONObject siteJson)) {
				throw new IllegalArgumentException(
					"perSite[" + i + "] must be a JSONObject");
			}

			long groupId = _requireLong(siteJson, "groupId");
			String siteName = _requireString(siteJson, "siteName");
			long created = _requireLong(siteJson, "created");
			long failed = _requireLong(siteJson, "failed");
			String error = _optionalString(siteJson, "error");

			Map<String, Object> item = new LinkedHashMap<>();

			item.put("groupId", groupId);
			item.put("siteName", siteName);
			item.put("requested", created + failed);
			item.put("count", created);
			item.put("skipped", failed);
			item.put("success", (failed == 0) && (error == null));
			item.put("created", created);
			item.put("failed", failed);

			if (error != null) {
				item.put("error", error);
			}

			items.add(item);
			skipped += failed;
		}

		long requested = _requireLong(jsonObject, "totalRequested");
		long count = _requireLong(jsonObject, "totalCreated");
		boolean success = _requireBoolean(jsonObject, "ok");
		String error = _resolveWebContentError(success, count, requested, items);

		return new WorkflowStepResult(
			success, requested, count, skipped, items, error);
	}

	private static boolean _requireBoolean(
		JSONObject jsonObject, String key) {

		Object value = _requireValue(jsonObject, key);

		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}

		throw new IllegalArgumentException(key + " must be a boolean");
	}

	private static JSONArray _requireJSONArray(
		JSONObject jsonObject, String key) {

		Object value = _requireValue(jsonObject, key);

		if (value instanceof JSONArray jsonArray) {
			return jsonArray;
		}

		throw new IllegalArgumentException(key + " must be a JSONArray");
	}

	private static long _requireLong(JSONObject jsonObject, String key) {
		Object value = _requireValue(jsonObject, key);

		if (value instanceof Number number) {
			return number.longValue();
		}

		throw new IllegalArgumentException(key + " must be a number");
	}

	private static JSONObject _requireJSONObject(Object creatorResult) {
		if (!(creatorResult instanceof JSONObject jsonObject)) {
			throw new IllegalArgumentException(
				"Creator result must be a JSONObject");
		}

		return jsonObject;
	}

	private static String _requireString(JSONObject jsonObject, String key) {
		Object value = _requireValue(jsonObject, key);

		if (value instanceof String string) {
			return string;
		}

		throw new IllegalArgumentException(key + " must be a string");
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

	private static String _resolveWebContentError(
		boolean success, long count, long requested,
		List<Map<String, Object>> items) {

		if (success) {
			return null;
		}

		for (Map<String, Object> item : items) {
			Object error = item.get("error");

			if (error instanceof String errorMessage) {
				return errorMessage;
			}
		}

		if (count == 0) {
			return "No web contents were created";
		}

		return "Only " + count + " of " + requested +
			" web contents were created.";
	}

	private static List<Map<String, Object>> _toList(JSONArray jsonArray) {
		List<Map<String, Object>> items = new ArrayList<>(jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {
			Object value = jsonArray.get(i);

			if (!(value instanceof JSONObject jsonObject)) {
				throw new IllegalArgumentException(
					"items[" + i + "] must be a JSONObject");
			}

			items.add(_toMap(jsonObject));
		}

		return items;
	}

	private static Map<String, Object> _toMap(JSONObject jsonObject) {
		Map<String, Object> normalized = new LinkedHashMap<>();

		for (String key : jsonObject.keySet()) {
			normalized.put(key, _normalizeValue(jsonObject.get(key)));
		}

		return normalized;
	}

	private static Object _normalizeValue(Object value) {
		if (value instanceof JSONArray jsonArray) {
			return _toList(jsonArray);
		}

		if (value instanceof JSONObject jsonObject) {
			return _toMap(jsonObject);
		}

		return value;
	}

}
