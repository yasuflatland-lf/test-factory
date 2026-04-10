package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupLocalServiceUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/data"
	},
	service = MVCResourceCommand.class
)
public class DataListResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		String type = ParamUtil.getString(httpServletRequest, "type");

		long companyId = _portal.getCompanyId(httpServletRequest);

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		switch (type) {
			case "organizations":
				List<Organization> organizations =
					OrganizationLocalServiceUtil.getOrganizations(
						companyId,
						OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID,
						QueryUtil.ALL_POS, QueryUtil.ALL_POS);

				for (Organization organization : organizations) {
					JSONObject jsonObject =
						JSONFactoryUtil.createJSONObject();

					jsonObject.put("label", organization.getName());
					jsonObject.put(
						"value",
						String.valueOf(organization.getOrganizationId()));

					jsonArray.put(jsonObject);
				}

				break;
			case "roles":
				List<Role> roles = RoleLocalServiceUtil.getRoles(
					companyId,
					new int[] {RoleConstants.TYPE_REGULAR});

				for (Role role : roles) {
					JSONObject jsonObject =
						JSONFactoryUtil.createJSONObject();

					jsonObject.put("label", role.getName());
					jsonObject.put(
						"value",
						String.valueOf(role.getRoleId()));

					jsonArray.put(jsonObject);
				}

				break;
			case "user-groups":
				List<UserGroup> userGroups =
					UserGroupLocalServiceUtil.getUserGroups(companyId);

				for (UserGroup userGroup : userGroups) {
					JSONObject jsonObject =
						JSONFactoryUtil.createJSONObject();

					jsonObject.put("label", userGroup.getName());
					jsonObject.put(
						"value",
						String.valueOf(
							userGroup.getUserGroupId()));

					jsonArray.put(jsonObject);
				}

				break;
			default:
				break;
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, jsonArray);
	}

	@Reference
	private Portal _portal;

}
