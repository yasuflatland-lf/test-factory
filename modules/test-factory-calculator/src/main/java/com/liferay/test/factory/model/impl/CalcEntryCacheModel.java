/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.model.impl;

import com.liferay.petra.lang.HashUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.CacheModel;
import com.liferay.test.factory.model.CalcEntry;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Date;

/**
 * The cache model class for representing CalcEntry in entity cache.
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
public class CalcEntryCacheModel
	implements CacheModel<CalcEntry>, Externalizable {

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof CalcEntryCacheModel)) {
			return false;
		}

		CalcEntryCacheModel calcEntryCacheModel = (CalcEntryCacheModel)object;

		if (calcEntryId == calcEntryCacheModel.calcEntryId) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return HashUtil.hash(0, calcEntryId);
	}

	@Override
	public String toString() {
		StringBundler sb = new StringBundler(21);

		sb.append("{calcEntryId=");
		sb.append(calcEntryId);
		sb.append(", companyId=");
		sb.append(companyId);
		sb.append(", userId=");
		sb.append(userId);
		sb.append(", userName=");
		sb.append(userName);
		sb.append(", createDate=");
		sb.append(createDate);
		sb.append(", modifiedDate=");
		sb.append(modifiedDate);
		sb.append(", num1=");
		sb.append(num1);
		sb.append(", num2=");
		sb.append(num2);
		sb.append(", operator=");
		sb.append(operator);
		sb.append(", result=");
		sb.append(result);
		sb.append("}");

		return sb.toString();
	}

	@Override
	public CalcEntry toEntityModel() {
		CalcEntryImpl calcEntryImpl = new CalcEntryImpl();

		calcEntryImpl.setCalcEntryId(calcEntryId);
		calcEntryImpl.setCompanyId(companyId);
		calcEntryImpl.setUserId(userId);

		if (userName == null) {
			calcEntryImpl.setUserName("");
		}
		else {
			calcEntryImpl.setUserName(userName);
		}

		if (createDate == Long.MIN_VALUE) {
			calcEntryImpl.setCreateDate(null);
		}
		else {
			calcEntryImpl.setCreateDate(new Date(createDate));
		}

		if (modifiedDate == Long.MIN_VALUE) {
			calcEntryImpl.setModifiedDate(null);
		}
		else {
			calcEntryImpl.setModifiedDate(new Date(modifiedDate));
		}

		calcEntryImpl.setNum1(num1);
		calcEntryImpl.setNum2(num2);

		if (operator == null) {
			calcEntryImpl.setOperator("");
		}
		else {
			calcEntryImpl.setOperator(operator);
		}

		calcEntryImpl.setResult(result);

		calcEntryImpl.resetOriginalValues();

		return calcEntryImpl;
	}

	@Override
	public void readExternal(ObjectInput objectInput) throws IOException {
		calcEntryId = objectInput.readLong();

		companyId = objectInput.readLong();

		userId = objectInput.readLong();
		userName = objectInput.readUTF();
		createDate = objectInput.readLong();
		modifiedDate = objectInput.readLong();

		num1 = objectInput.readDouble();

		num2 = objectInput.readDouble();
		operator = objectInput.readUTF();

		result = objectInput.readDouble();
	}

	@Override
	public void writeExternal(ObjectOutput objectOutput) throws IOException {
		objectOutput.writeLong(calcEntryId);

		objectOutput.writeLong(companyId);

		objectOutput.writeLong(userId);

		if (userName == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(userName);
		}

		objectOutput.writeLong(createDate);
		objectOutput.writeLong(modifiedDate);

		objectOutput.writeDouble(num1);

		objectOutput.writeDouble(num2);

		if (operator == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(operator);
		}

		objectOutput.writeDouble(result);
	}

	public long calcEntryId;
	public long companyId;
	public long userId;
	public String userName;
	public long createDate;
	public long modifiedDate;
	public double num1;
	public double num2;
	public String operator;
	public double result;

}