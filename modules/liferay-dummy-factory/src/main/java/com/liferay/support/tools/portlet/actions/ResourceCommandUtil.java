package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.support.tools.service.BatchSpec;

class ResourceCommandUtil {

	static BatchSpec parseBatchSpec(JSONObject data) {
		return new BatchSpec(
			GetterUtil.getInteger(data.getString("count")),
			data.getString("baseName"));
	}

	static void setErrorResponse(JSONObject responseJson, Throwable throwable) {
		String message = throwable.getMessage();

		responseJson.put(
			"error",
			(message != null) ? message : "An unexpected error occurred");
		responseJson.put("success", false);
	}

}
