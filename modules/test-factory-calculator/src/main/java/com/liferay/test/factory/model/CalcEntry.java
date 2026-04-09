/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.model;

import com.liferay.portal.kernel.annotation.ImplementationClassName;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.util.Accessor;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The extended model interface for the CalcEntry service. Represents a row in the &quot;TestFactory_CalcEntry&quot; database table, with each column mapped to a property of this class.
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntryModel
 * @generated
 */
@ImplementationClassName("com.liferay.test.factory.model.impl.CalcEntryImpl")
@ProviderType
public interface CalcEntry extends CalcEntryModel, PersistedModel {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this interface directly. Add methods to <code>com.liferay.test.factory.model.impl.CalcEntryImpl</code> and rerun ServiceBuilder to automatically copy the method declarations to this interface.
	 */
	public static final Accessor<CalcEntry, Long> CALC_ENTRY_ID_ACCESSOR =
		new Accessor<CalcEntry, Long>() {

			@Override
			public Long get(CalcEntry calcEntry) {
				return calcEntry.getCalcEntryId();
			}

			@Override
			public Class<Long> getAttributeClass() {
				return Long.class;
			}

			@Override
			public Class<CalcEntry> getTypeClass() {
				return CalcEntry.class;
			}

		};

}