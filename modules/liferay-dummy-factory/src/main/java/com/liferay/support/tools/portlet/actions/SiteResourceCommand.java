package com.liferay.support.tools.portlet.actions;

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
import com.liferay.support.tools.service.SiteCreator;
import com.liferay.support.tools.service.SiteMembershipType;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/site"
	},
	service = MVCResourceCommand.class
)
public class SiteResourceCommand extends BaseMVCResourceCommand {

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

			String membershipTypeString = GetterUtil.getString(
				data.getString("membershipType"), "open");

			SiteMembershipType membershipType =
				SiteMembershipType.fromString(membershipTypeString);

			long parentGroupId = GetterUtil.getLong(
				data.getString("parentGroupId"));
			long siteTemplateId = GetterUtil.getLong(
				data.getString("siteTemplateId"));
			boolean manualMembership = GetterUtil.getBoolean(
				data.getString("manualMembership"), true);
			boolean inheritContent = GetterUtil.getBoolean(
				data.getString("inheritContent"));
			boolean active = GetterUtil.getBoolean(
				data.getString("active"), true);
			String description = data.getString("description");

			long userId = _portal.getUserId(resourceRequest);
			long companyId = _portal.getCompanyId(resourceRequest);

			responseJson = _siteCreator.create(
				userId, companyId, batchSpec,
				membershipType, parentGroupId,
				siteTemplateId, manualMembership,
				inheritContent, active, description);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create sites", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteResourceCommand.class);

	@Reference
	private Portal _portal;

	@Reference
	private SiteCreator _siteCreator;

}
