/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.service;

import com.liferay.portal.kernel.service.ServiceWrapper;

/**
 * Provides a wrapper for {@link CalcEntryService}.
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntryService
 * @generated
 */
public class CalcEntryServiceWrapper
	implements CalcEntryService, ServiceWrapper<CalcEntryService> {

	public CalcEntryServiceWrapper() {
		this(null);
	}

	public CalcEntryServiceWrapper(CalcEntryService calcEntryService) {
		_calcEntryService = calcEntryService;
	}

	@Override
	public com.liferay.test.factory.model.CalcEntry calculate(
			double num1, double num2, String operator,
			com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryService.calculate(
			num1, num2, operator, serviceContext);
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return _calcEntryService.getOSGiServiceIdentifier();
	}

	@Override
	public CalcEntryService getWrappedService() {
		return _calcEntryService;
	}

	@Override
	public void setWrappedService(CalcEntryService calcEntryService) {
		_calcEntryService = calcEntryService;
	}

	private CalcEntryService _calcEntryService;

}