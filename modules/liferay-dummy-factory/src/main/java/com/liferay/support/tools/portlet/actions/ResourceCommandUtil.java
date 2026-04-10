package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.util.Validator;

class ResourceCommandUtil {

	static String validate(int count, String baseName) {
		if (count <= 0) {
			return "count must be greater than 0";
		}

		if (Validator.isNull(baseName)) {
			return "baseName is required";
		}

		return null;
	}

	static final TransactionConfig TRANSACTION_CONFIG =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

}
