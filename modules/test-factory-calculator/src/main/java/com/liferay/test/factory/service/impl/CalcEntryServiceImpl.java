package com.liferay.test.factory.service.impl;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.test.factory.model.CalcEntry;
import com.liferay.test.factory.service.base.CalcEntryServiceBaseImpl;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"json.web.service.context.name=TestFactory",
		"json.web.service.context.path=CalcEntry"
	},
	service = AopService.class
)
public class CalcEntryServiceImpl extends CalcEntryServiceBaseImpl {

	public CalcEntry calculate(
			double num1, double num2, String operator,
			ServiceContext serviceContext)
		throws PortalException {

		return calcEntryLocalService.addCalcEntry(
			serviceContext.getUserId(), num1, num2, operator, serviceContext);
	}

}
