package com.liferay.test.factory.service.impl;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.test.factory.model.CalcEntry;
import com.liferay.test.factory.service.base.CalcEntryLocalServiceBaseImpl;

import java.util.Date;

import org.osgi.service.component.annotations.Component;

@Component(
	property = "model.class.name=com.liferay.test.factory.model.CalcEntry",
	service = AopService.class
)
public class CalcEntryLocalServiceImpl extends CalcEntryLocalServiceBaseImpl {

	public CalcEntry addCalcEntry(
			long userId, double num1, double num2, String operator,
			ServiceContext serviceContext)
		throws PortalException {

		double result = _calculate(num1, num2, operator);

		long calcEntryId = counterLocalService.increment();

		CalcEntry calcEntry = calcEntryPersistence.create(calcEntryId);

		User user = userLocalService.getUser(userId);

		calcEntry.setCompanyId(serviceContext.getCompanyId());
		calcEntry.setUserId(userId);
		calcEntry.setUserName(user.getFullName());
		calcEntry.setCreateDate(new Date());
		calcEntry.setModifiedDate(new Date());
		calcEntry.setNum1(num1);
		calcEntry.setNum2(num2);
		calcEntry.setOperator(operator);
		calcEntry.setResult(result);

		return calcEntryPersistence.update(calcEntry);
	}

	private double _calculate(double num1, double num2, String operator)
		throws PortalException {

		switch (operator) {
			case "+":
				return num1 + num2;
			case "-":
				return num1 - num2;
			case "*":
				return num1 * num2;
			case "/":
				if (num2 == 0) {
					throw new PortalException("Division by zero");
				}

				return num1 / num2;
			default:
				throw new PortalException(
					"Invalid operator: " + operator);
		}
	}

}
