/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.model;

import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.base.BaseTable;

import java.sql.Types;

import java.util.Date;

/**
 * The table class for the &quot;TestFactory_CalcEntry&quot; database table.
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntry
 * @generated
 */
public class CalcEntryTable extends BaseTable<CalcEntryTable> {

	public static final CalcEntryTable INSTANCE = new CalcEntryTable();

	public final Column<CalcEntryTable, Long> calcEntryId = createColumn(
		"calcEntryId", Long.class, Types.BIGINT, Column.FLAG_PRIMARY);
	public final Column<CalcEntryTable, Long> companyId = createColumn(
		"companyId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, Long> userId = createColumn(
		"userId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, String> userName = createColumn(
		"userName", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, Date> createDate = createColumn(
		"createDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, Date> modifiedDate = createColumn(
		"modifiedDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, Double> num1 = createColumn(
		"num1", Double.class, Types.DOUBLE, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, Double> num2 = createColumn(
		"num2", Double.class, Types.DOUBLE, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, String> operator = createColumn(
		"operator", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<CalcEntryTable, Double> result = createColumn(
		"result", Double.class, Types.DOUBLE, Column.FLAG_DEFAULT);

	private CalcEntryTable() {
		super("TestFactory_CalcEntry", CalcEntryTable::new);
	}

}