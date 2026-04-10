package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.RoleCreator;
import com.liferay.support.tools.service.RoleType;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/role"
	},
	service = MVCResourceCommand.class
)
public class RoleResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		String dataString = ParamUtil.getString(
			httpServletRequest, "data");

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		try {
			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			int count = GetterUtil.getInteger(
				data.getString("count"));
			String baseName = data.getString("baseName");

			BatchSpec batchSpec = new BatchSpec(count, baseName);

			String roleTypeString = GetterUtil.getString(
				data.getString("roleType"), "regular");
			String description = GetterUtil.getString(
				data.getString("description"), "");

			RoleType roleType = RoleType.fromString(roleTypeString);

			long userId = _portal.getUserId(resourceRequest);

			responseJson = TransactionInvokerUtil.invoke(
				ResourceCommandUtil.TRANSACTION_CONFIG,
				() -> _roleCreator.create(
					userId, batchSpec,
					roleType, description));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			responseJson.put("error", illegalArgumentException.getMessage());
			responseJson.put("success", false);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create roles", throwable);

			responseJson.put(
				"error",
				(throwable.getMessage() != null)
					? throwable.getMessage() : "An unexpected error occurred");
			responseJson.put("success", false);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RoleResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private RoleCreator _roleCreator;

}
