package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.UserCreator;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/user"
	},
	service = MVCResourceCommand.class
)
public class UserResourceCommand extends BaseMVCResourceCommand {

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

			if (count <= 0) {
				responseJson.put("error", "count must be greater than 0");
				responseJson.put("success", false);

				JSONPortletResponseUtil.writeJSON(
					resourceRequest, resourceResponse, responseJson);

				return;
			}

			if (Validator.isNull(baseName)) {
				responseJson.put("error", "baseName is required");
				responseJson.put("success", false);

				JSONPortletResponseUtil.writeJSON(
					resourceRequest, resourceResponse, responseJson);

				return;
			}

			String emailDomain = GetterUtil.getString(
				data.getString("emailDomain"), "liferay.com");
			String password = GetterUtil.getString(
				data.getString("password"), "test");
			boolean male = GetterUtil.getBoolean(
				data.getString("male"), true);
			String jobTitle = GetterUtil.getString(
				data.getString("jobTitle"), "");

			long[] organizationIds = _toLongArray(
				data.getJSONArray("organizationIds"));
			long[] roleIds = _toLongArray(data.getJSONArray("roleIds"));
			long[] userGroupIds = _toLongArray(
				data.getJSONArray("userGroupIds"));

			long userId = _portal.getUserId(resourceRequest);
			long companyId = _portal.getCompanyId(resourceRequest);

			responseJson = TransactionInvokerUtil.invoke(
				_transactionConfig,
				() -> _userCreator.create(
					userId, companyId, count, baseName,
					emailDomain, password, male, jobTitle,
					organizationIds, roleIds, userGroupIds));
		}
		catch (Throwable throwable) {
			_log.error("Failed to create users", throwable);

			responseJson.put(
				"error",
				(throwable.getMessage() != null)
					? throwable.getMessage() : "An unexpected error occurred");
			responseJson.put("success", false);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private long[] _toLongArray(JSONArray jsonArray) {
		if ((jsonArray == null) || (jsonArray.length() == 0)) {
			return new long[0];
		}

		long[] result = new long[jsonArray.length()];

		for (int i = 0; i < jsonArray.length(); i++) {
			result[i] = GetterUtil.getLong(jsonArray.get(i));
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserResourceCommand.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private Portal _portal;

	@Reference
	private UserCreator _userCreator;

}
