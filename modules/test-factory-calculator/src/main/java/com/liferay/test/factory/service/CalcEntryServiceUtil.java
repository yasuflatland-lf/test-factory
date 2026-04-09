/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.test.factory.model.CalcEntry;

/**
 * Provides the remote service utility for CalcEntry. This utility wraps
 * <code>com.liferay.test.factory.service.impl.CalcEntryServiceImpl</code> and is an
 * access point for service operations in application layer code running on a
 * remote server. Methods of this service are expected to have security checks
 * based on the propagated JAAS credentials because this service can be
 * accessed remotely.
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntryService
 * @generated
 */
public class CalcEntryServiceUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Add custom service methods to <code>com.liferay.test.factory.service.impl.CalcEntryServiceImpl</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static CalcEntry calculate(
			double num1, double num2, String operator,
			com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws PortalException {

		return getService().calculate(num1, num2, operator, serviceContext);
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	public static String getOSGiServiceIdentifier() {
		return getService().getOSGiServiceIdentifier();
	}

	public static CalcEntryService getService() {
		return _serviceSnapshot.get();
	}

	private static final Snapshot<CalcEntryService> _serviceSnapshot =
		new Snapshot<>(CalcEntryServiceUtil.class, CalcEntryService.class);

}