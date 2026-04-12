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

		JSONArray itemsArray = jsonObject.getJSONArray("items");

		List<Map<String, Object>> items = new ArrayList<>();

		if (itemsArray != null) {
			for (int i = 0; i < itemsArray.length(); i++) {
				Object value = itemsArray.get(i);

				if (value instanceof JSONObject itemJSONObject) {
					items.add(_toMap(itemJSONObject));
				}
				else {
					Map<String, Object> wrappedValue = new LinkedHashMap<>();

					wrappedValue.put("value", _normalizeValue(value));

					items.add(wrappedValue);
				}
			}
		}

		int count = jsonObject.has("count") ? jsonObject.getInt("count") :
			items.size();
		int requested = jsonObject.has("requested") ?
			jsonObject.getInt("requested") : count;
		int skipped = jsonObject.has("skipped") ?
			jsonObject.getInt("skipped") : 0;
		boolean success = jsonObject.has("success") ?
			jsonObject.getBoolean("success") : (count == requested);
		String error = jsonObject.has("error") ?
			jsonObject.getString("error") : null;

		return new WorkflowStepResult(
			requested, count, skipped, success, error, items);
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
