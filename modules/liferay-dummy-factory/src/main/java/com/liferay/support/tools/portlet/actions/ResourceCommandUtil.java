package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;

class ResourceCommandUtil {

	static void setErrorResponse(JSONObject responseJson, Throwable throwable) {
		String message = throwable.getMessage();

		responseJson.put(
			"error",
			(message != null) ? message : "An unexpected error occurred");
		responseJson.put("success", false);
	}

	static final TransactionConfig TRANSACTION_CONFIG =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

}
