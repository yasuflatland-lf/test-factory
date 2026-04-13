package com.liferay.support.tools.workflow.adapter.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.support.tools.workflow.adapter.TestJsonObjects;

import org.junit.jupiter.api.Test;

class WorkflowResultNormalizerTest {

	@Test
	void normalizeRejectsMissingRequiredFields() {
		assertThrows(
			IllegalArgumentException.class,
			() -> WorkflowResultNormalizer.normalize(_workflowResult(false)));
	}

	@Test
	void normalizeRejectsNonObjectItems() {
		JSONObject jsonObject = _workflowResult(true);
		JSONArray items = jsonObject.getJSONArray("items");

		items.put("broken");

		assertThrows(
			IllegalArgumentException.class,
			() -> WorkflowResultNormalizer.normalize(jsonObject));
	}

	private static JSONObject _workflowResult(boolean includeSuccess) {
		JSONObject item = TestJsonObjects.object();

		item.put("groupId", 101L);
		item.put("name", "Item 1");

		JSONArray items = TestJsonObjects.array();
		items.put(item);

		JSONObject jsonObject = TestJsonObjects.object();

		jsonObject.put("count", 1);
		jsonObject.put("items", items);
		jsonObject.put("requested", 1);
		jsonObject.put("skipped", 0);

		if (includeSuccess) {
			jsonObject.put("success", true);
		}

		return jsonObject;
	}

}
