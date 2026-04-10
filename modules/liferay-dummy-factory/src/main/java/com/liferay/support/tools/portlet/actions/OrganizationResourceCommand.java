package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;

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

		int numberOfOrganizations = ParamUtil.getInteger(
			httpServletRequest, "numberOfOrganizations");
		String baseOrganizationName = ParamUtil.getString(
			httpServletRequest, "baseOrganizationName");
		long parentOrganizationId = ParamUtil.getLong(
			httpServletRequest, "parentOrganizationId");
		boolean organizationSiteCreate = ParamUtil.getBoolean(
			httpServletRequest, "organizationSiteCreate");

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("baseOrganizationName", baseOrganizationName);
		jsonObject.put("numberOfOrganizations", numberOfOrganizations);
		jsonObject.put("organizationSiteCreate", organizationSiteCreate);
		jsonObject.put("parentOrganizationId", parentOrganizationId);
		jsonObject.put("success", true);

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, jsonObject);
	}

	@Reference
	private Portal _portal;

}
