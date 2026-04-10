package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.OrganizationCreator;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/org"
	},
	service = MVCResourceCommand.class
)
public class OrganizationResourceCommand extends BaseMVCResourceCommand {

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
			long parentOrganizationId = GetterUtil.getLong(
				data.getString("parentOrganizationId"));
			boolean site = GetterUtil.getBoolean(
				data.getString("site"));

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

			long userId = _portal.getUserId(resourceRequest);

			responseJson = _organizationCreator.create(
				userId, count, baseName,
				parentOrganizationId, site);
		}
		catch (Throwable throwable) {
			responseJson.put(
				"error",
				(throwable.getMessage() != null)
					? throwable.getMessage() : "An unexpected error occurred");
			responseJson.put("success", false);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	@Reference
	private OrganizationCreator _organizationCreator;

	@Reference
	private Portal _portal;

}
