package com.liferay.support.tools.service;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

public interface DataListProvider {

	default JSONObject createOption(String label, long value) {
		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("label", label);
		jsonObject.put("value", String.valueOf(value));

		return jsonObject;
	}

	JSONArray getOptions(long companyId, String type);

	String[] getSupportedTypes();

}
