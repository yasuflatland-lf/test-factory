package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchSpec;
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

			BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

			String emailDomain = data.getString("emailDomain", "liferay.com");

			if (emailDomain.isEmpty()) {
				emailDomain = "liferay.com";
			}

			String password = data.getString("password", "test");

			if (password.isEmpty()) {
				password = "test";
			}

			boolean male = data.has("male") ?
				data.getBoolean("male") : true;
			String jobTitle = data.getString("jobTitle", "");

			long[] organizationIds = _toLongArray(
				data.getJSONArray("organizationIds"));
			long[] roleIds = _toLongArray(data.getJSONArray("roleIds"));
			long[] userGroupIds = _toLongArray(
				data.getJSONArray("userGroupIds"));
			long[] siteRoleIds = _toLongArray(
				data.getJSONArray("siteRoleIds"));
			long[] orgRoleIds = _toLongArray(
				data.getJSONArray("orgRoleIds"));

			boolean fakerEnable = GetterUtil.getBoolean(
				data.getString("fakerEnable"), false);
			String locale = GetterUtil.getString(
				data.getString("locale"), "en_US");
			boolean generatePersonalSiteLayouts = GetterUtil.getBoolean(
				data.getString("generatePersonalSiteLayouts"), false);
			long publicLayoutSetPrototypeId = GetterUtil.getLong(
				data.getString("publicLayoutSetPrototypeId"), 0L);
			long privateLayoutSetPrototypeId = GetterUtil.getLong(
				data.getString("privateLayoutSetPrototypeId"), 0L);
			long[] groupIds = _toLongArray(data.getJSONArray("groupIds"));

			long userId = _portal.getUserId(resourceRequest);
			long companyId = _portal.getCompanyId(resourceRequest);

			responseJson = _userCreator.create(
				userId, companyId, batchSpec,
				emailDomain, password, male, jobTitle,
				organizationIds, roleIds, userGroupIds,
				siteRoleIds, orgRoleIds, fakerEnable, locale,
				generatePersonalSiteLayouts, publicLayoutSetPrototypeId,
				privateLayoutSetPrototypeId, groupIds);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create users", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
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

	@Reference
	private Portal _portal;

	@Reference
	private UserCreator _userCreator;

}
