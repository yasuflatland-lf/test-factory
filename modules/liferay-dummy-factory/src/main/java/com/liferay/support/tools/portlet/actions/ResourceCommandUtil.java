package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONObject;

class ResourceCommandUtil {

	static void setErrorResponse(JSONObject responseJson, Throwable throwable) {
		String message = throwable.getMessage();

		responseJson.put(
			"error",
			(message != null) ? message : "An unexpected error occurred");
		responseJson.put("success", false);
	}

}
