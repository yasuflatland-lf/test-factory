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

		return new WorkflowStepResult(
			jsonObject.getBoolean("success"),
			jsonObject.getLong("requested"),
			jsonObject.getLong("count"),
			jsonObject.getLong("skipped"),
			_toList(jsonObject.getJSONArray("items")),
			jsonObject.getString("error", null));
	}

	public static WorkflowStepResult normalizeWebContentResult(
		Object creatorResult) {

		JSONObject jsonObject = _requireJSONObject(creatorResult);
		JSONArray perSite = jsonObject.getJSONArray("perSite");
		List<Map<String, Object>> items = new ArrayList<>();
		long skipped = 0;

		if (perSite != null) {
			for (int i = 0; i < perSite.length(); i++) {
				JSONObject siteJson = perSite.getJSONObject(i);
				long created = siteJson.getLong("created");
				long failed = siteJson.getLong("failed");
				String error = siteJson.getString("error", null);

				Map<String, Object> item = new LinkedHashMap<>();

				item.put("groupId", siteJson.getLong("groupId"));
				item.put("siteName", siteJson.getString("siteName"));
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
		}

		long requested = jsonObject.getLong("totalRequested");
		long count = jsonObject.getLong("totalCreated");
		boolean success = jsonObject.getBoolean("ok");

		return new WorkflowStepResult(
			success, requested, count, skipped, items,
			_resolveWebContentError(success, count, requested, items));
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

	private static JSONObject _requireJSONObject(Object creatorResult) {
		if (!(creatorResult instanceof JSONObject jsonObject)) {
			throw new IllegalArgumentException(
				"Creator result must be a JSONObject");
		}

		return jsonObject;
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
		List<Map<String, Object>> items = new ArrayList<>();

		if (jsonArray == null) {
			return items;
		}

		for (int i = 0; i < jsonArray.length(); i++) {
			Object value = jsonArray.get(i);

			if (value instanceof JSONObject jsonObject) {
				items.add(_toMap(jsonObject));
			}
			else {
				Map<String, Object> item = new LinkedHashMap<>();

				item.put("value", _normalizeValue(value));

				items.add(item);
			}
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

}
