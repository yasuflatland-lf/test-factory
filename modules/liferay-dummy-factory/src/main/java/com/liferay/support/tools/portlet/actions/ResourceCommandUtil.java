package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;

class ResourceCommandUtil {

	static final TransactionConfig TRANSACTION_CONFIG =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

}
