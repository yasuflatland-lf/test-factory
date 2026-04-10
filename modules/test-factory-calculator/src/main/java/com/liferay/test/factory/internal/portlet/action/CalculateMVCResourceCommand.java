package com.liferay.test.factory.internal.portlet.action;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.test.factory.constants.TestFactoryPortletKeys;

import org.osgi.service.component.annotations.Reference;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"javax.portlet.name=" + TestFactoryPortletKeys.TEST_FACTORY,
		"mvc.command.name=/test-factory/calculate"
	},
	service = MVCResourceCommand.class
)
public class CalculateMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		double num1 = ParamUtil.getDouble(httpServletRequest, "num1");
		double num2 = ParamUtil.getDouble(httpServletRequest, "num2");
		String operator = ParamUtil.getString(
			httpServletRequest, "operator");

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		try {
			double result = _calculate(num1, num2, operator);

			jsonObject.put("result", result);
		}
		catch (Exception exception) {
			jsonObject.put("error", exception.getMessage());
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, jsonObject);
	}

	@Reference
	private Portal _portal;

	private double _calculate(double num1, double num2, String operator)
		throws Exception {

		switch (operator) {
			case "+":
				return num1 + num2;
			case "-":
				return num1 - num2;
			case "*":
				return num1 * num2;
			case "/":
				if (num2 == 0) {
					throw new Exception("Division by zero");
				}

				return num1 / num2;
			default:
				throw new Exception("Invalid operator: " + operator);
		}
	}

}
