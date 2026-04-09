/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.model;

import com.liferay.portal.kernel.model.ModelWrapper;
import com.liferay.portal.kernel.model.wrapper.BaseModelWrapper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is a wrapper for {@link CalcEntry}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntry
 * @generated
 */
public class CalcEntryWrapper
	extends BaseModelWrapper<CalcEntry>
	implements CalcEntry, ModelWrapper<CalcEntry> {

	public CalcEntryWrapper(CalcEntry calcEntry) {
		super(calcEntry);
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put("calcEntryId", getCalcEntryId());
		attributes.put("companyId", getCompanyId());
		attributes.put("userId", getUserId());
		attributes.put("userName", getUserName());
		attributes.put("createDate", getCreateDate());
		attributes.put("modifiedDate", getModifiedDate());
		attributes.put("num1", getNum1());
		attributes.put("num2", getNum2());
		attributes.put("operator", getOperator());
		attributes.put("result", getResult());

		return attributes;
	}

	@Override
	public void setModelAttributes(Map<String, Object> attributes) {
		Long calcEntryId = (Long)attributes.get("calcEntryId");

		if (calcEntryId != null) {
			setCalcEntryId(calcEntryId);
		}

		Long companyId = (Long)attributes.get("companyId");

		if (companyId != null) {
			setCompanyId(companyId);
		}

		Long userId = (Long)attributes.get("userId");

		if (userId != null) {
			setUserId(userId);
		}

		String userName = (String)attributes.get("userName");

		if (userName != null) {
			setUserName(userName);
		}

		Date createDate = (Date)attributes.get("createDate");

		if (createDate != null) {
			setCreateDate(createDate);
		}

		Date modifiedDate = (Date)attributes.get("modifiedDate");

		if (modifiedDate != null) {
			setModifiedDate(modifiedDate);
		}

		Double num1 = (Double)attributes.get("num1");

		if (num1 != null) {
			setNum1(num1);
		}

		Double num2 = (Double)attributes.get("num2");

		if (num2 != null) {
			setNum2(num2);
		}

		String operator = (String)attributes.get("operator");

		if (operator != null) {
			setOperator(operator);
		}

		Double result = (Double)attributes.get("result");

		if (result != null) {
			setResult(result);
		}
	}

	@Override
	public CalcEntry cloneWithOriginalValues() {
		return wrap(model.cloneWithOriginalValues());
	}

	/**
	 * Returns the calc entry ID of this calc entry.
	 *
	 * @return the calc entry ID of this calc entry
	 */
	@Override
	public long getCalcEntryId() {
		return model.getCalcEntryId();
	}

	/**
	 * Returns the company ID of this calc entry.
	 *
	 * @return the company ID of this calc entry
	 */
	@Override
	public long getCompanyId() {
		return model.getCompanyId();
	}

	/**
	 * Returns the create date of this calc entry.
	 *
	 * @return the create date of this calc entry
	 */
	@Override
	public Date getCreateDate() {
		return model.getCreateDate();
	}

	/**
	 * Returns the modified date of this calc entry.
	 *
	 * @return the modified date of this calc entry
	 */
	@Override
	public Date getModifiedDate() {
		return model.getModifiedDate();
	}

	/**
	 * Returns the num1 of this calc entry.
	 *
	 * @return the num1 of this calc entry
	 */
	@Override
	public double getNum1() {
		return model.getNum1();
	}

	/**
	 * Returns the num2 of this calc entry.
	 *
	 * @return the num2 of this calc entry
	 */
	@Override
	public double getNum2() {
		return model.getNum2();
	}

	/**
	 * Returns the operator of this calc entry.
	 *
	 * @return the operator of this calc entry
	 */
	@Override
	public String getOperator() {
		return model.getOperator();
	}

	/**
	 * Returns the primary key of this calc entry.
	 *
	 * @return the primary key of this calc entry
	 */
	@Override
	public long getPrimaryKey() {
		return model.getPrimaryKey();
	}

	/**
	 * Returns the result of this calc entry.
	 *
	 * @return the result of this calc entry
	 */
	@Override
	public double getResult() {
		return model.getResult();
	}

	/**
	 * Returns the user ID of this calc entry.
	 *
	 * @return the user ID of this calc entry
	 */
	@Override
	public long getUserId() {
		return model.getUserId();
	}

	/**
	 * Returns the user name of this calc entry.
	 *
	 * @return the user name of this calc entry
	 */
	@Override
	public String getUserName() {
		return model.getUserName();
	}

	/**
	 * Returns the user uuid of this calc entry.
	 *
	 * @return the user uuid of this calc entry
	 */
	@Override
	public String getUserUuid() {
		return model.getUserUuid();
	}

	@Override
	public void persist() {
		model.persist();
	}

	/**
	 * Sets the calc entry ID of this calc entry.
	 *
	 * @param calcEntryId the calc entry ID of this calc entry
	 */
	@Override
	public void setCalcEntryId(long calcEntryId) {
		model.setCalcEntryId(calcEntryId);
	}

	/**
	 * Sets the company ID of this calc entry.
	 *
	 * @param companyId the company ID of this calc entry
	 */
	@Override
	public void setCompanyId(long companyId) {
		model.setCompanyId(companyId);
	}

	/**
	 * Sets the create date of this calc entry.
	 *
	 * @param createDate the create date of this calc entry
	 */
	@Override
	public void setCreateDate(Date createDate) {
		model.setCreateDate(createDate);
	}

	/**
	 * Sets the modified date of this calc entry.
	 *
	 * @param modifiedDate the modified date of this calc entry
	 */
	@Override
	public void setModifiedDate(Date modifiedDate) {
		model.setModifiedDate(modifiedDate);
	}

	/**
	 * Sets the num1 of this calc entry.
	 *
	 * @param num1 the num1 of this calc entry
	 */
	@Override
	public void setNum1(double num1) {
		model.setNum1(num1);
	}

	/**
	 * Sets the num2 of this calc entry.
	 *
	 * @param num2 the num2 of this calc entry
	 */
	@Override
	public void setNum2(double num2) {
		model.setNum2(num2);
	}

	/**
	 * Sets the operator of this calc entry.
	 *
	 * @param operator the operator of this calc entry
	 */
	@Override
	public void setOperator(String operator) {
		model.setOperator(operator);
	}

	/**
	 * Sets the primary key of this calc entry.
	 *
	 * @param primaryKey the primary key of this calc entry
	 */
	@Override
	public void setPrimaryKey(long primaryKey) {
		model.setPrimaryKey(primaryKey);
	}

	/**
	 * Sets the result of this calc entry.
	 *
	 * @param result the result of this calc entry
	 */
	@Override
	public void setResult(double result) {
		model.setResult(result);
	}

	/**
	 * Sets the user ID of this calc entry.
	 *
	 * @param userId the user ID of this calc entry
	 */
	@Override
	public void setUserId(long userId) {
		model.setUserId(userId);
	}

	/**
	 * Sets the user name of this calc entry.
	 *
	 * @param userName the user name of this calc entry
	 */
	@Override
	public void setUserName(String userName) {
		model.setUserName(userName);
	}

	/**
	 * Sets the user uuid of this calc entry.
	 *
	 * @param userUuid the user uuid of this calc entry
	 */
	@Override
	public void setUserUuid(String userUuid) {
		model.setUserUuid(userUuid);
	}

	@Override
	public String toXmlString() {
		return model.toXmlString();
	}

	@Override
	protected CalcEntryWrapper wrap(CalcEntry calcEntry) {
		return new CalcEntryWrapper(calcEntry);
	}

}