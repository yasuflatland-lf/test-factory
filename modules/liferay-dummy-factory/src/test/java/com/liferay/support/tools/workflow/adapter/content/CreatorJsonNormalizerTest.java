package com.liferay.support.tools.workflow.adapter.content;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.support.tools.workflow.adapter.TestJsonObjects;

import org.junit.jupiter.api.Test;

class CreatorJsonNormalizerTest {

	@Test
	void normalizeStandardResultRejectsMissingRequiredFields() {
		assertThrows(
			IllegalArgumentException.class,
			() -> CreatorJsonNormalizer.normalizeStandardResult(
				_standardResult(false)));
	}

	@Test
	void normalizeWebContentResultRejectsMalformedPerSiteItems() {
		assertThrows(
			IllegalArgumentException.class,
			() -> CreatorJsonNormalizer.normalizeWebContentResult(
				_webContentResult(false)));
	}

	private static JSONObject _standardResult(boolean includeSkipped) {
		JSONObject item = TestJsonObjects.object();

		item.put("roleId", 101L);
		item.put("name", "Role 1");

		JSONArray items = TestJsonObjects.array();
		items.put(item);

		JSONObject jsonObject = TestJsonObjects.object();

		jsonObject.put("count", 1);
		jsonObject.put("items", items);
		jsonObject.put("requested", 1);
		jsonObject.put("success", true);

		if (includeSkipped) {
			jsonObject.put("skipped", 0);
		}

		return jsonObject;
	}

	private static JSONObject _webContentResult(boolean includeCreated) {
		JSONObject siteJson = TestJsonObjects.object();

		siteJson.put("failed", 0);
		siteJson.put("groupId", 201L);
		siteJson.put("siteName", "Site 1");

		if (includeCreated) {
			siteJson.put("created", 1);
		}

		JSONArray perSite = TestJsonObjects.array();
		perSite.put(siteJson);

		JSONObject jsonObject = TestJsonObjects.object();

		jsonObject.put("ok", true);
		jsonObject.put("perSite", perSite);
		jsonObject.put("totalCreated", 1);
		jsonObject.put("totalRequested", 1);

		return jsonObject;
	}

}
