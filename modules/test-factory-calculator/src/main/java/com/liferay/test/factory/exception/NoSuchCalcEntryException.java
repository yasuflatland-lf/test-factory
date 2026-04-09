/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
package com.liferay.test.factory.exception;

import com.liferay.portal.kernel.exception.NoSuchModelException;

/**
 * @author Brian Wing Shun Chan
 */
public class NoSuchCalcEntryException extends NoSuchModelException {

	public NoSuchCalcEntryException() {
	}

	public NoSuchCalcEntryException(String msg) {
		super(msg);
	}

	public NoSuchCalcEntryException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public NoSuchCalcEntryException(Throwable throwable) {
		super(throwable);
	}

}