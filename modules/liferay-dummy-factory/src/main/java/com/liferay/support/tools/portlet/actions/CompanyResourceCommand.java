package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.CompanyCreator;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/company"
	},
	service = MVCResourceCommand.class
)
public class CompanyResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		String dataString = ParamUtil.getString(httpServletRequest, "data");

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		ProgressManager progressManager = new ProgressManager();

		progressManager.start(resourceRequest);

		try {
			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			int count = GetterUtil.getInteger(data.getString("count"));
			String webId = GetterUtil.getString(data.getString("webId"));
			String virtualHostname = GetterUtil.getString(
				data.getString("virtualHostname"));
			String mx = GetterUtil.getString(data.getString("mx"));
			int maxUsers = GetterUtil.getInteger(
				data.getString("maxUsers"), 0);
			boolean active = GetterUtil.getBoolean(
				data.getString("active"), true);

			ResourceCommandUtil.validateCount(count);

			if (webId.isEmpty()) {
				throw new IllegalArgumentException(
					"webId must not be empty");
			}

			if (virtualHostname.isEmpty()) {
				throw new IllegalArgumentException(
					"virtualHostname must not be empty");
			}

			if (mx.isEmpty()) {
				throw new IllegalArgumentException("mx must not be empty");
			}

			List<Company> companies = _companyCreator.create(
				count, webId, virtualHostname, mx, maxUsers, active,
				(current, total) -> progressManager.trackProgress(
					current, total));

			int createdCount = companies.size();
			boolean success = (createdCount == count);

			JSONArray itemsArray = JSONFactoryUtil.createJSONArray();

			for (Company company : companies) {
				JSONObject itemJson = JSONFactoryUtil.createJSONObject();

				itemJson.put("companyId", company.getCompanyId());
				itemJson.put("webId", company.getWebId());

				itemsArray.put(itemJson);
			}

			responseJson.put("count", createdCount);
			responseJson.put("items", itemsArray);
			responseJson.put("requested", count);
			responseJson.put("skipped", 0);
			responseJson.put("success", success);

			if (!success) {
				responseJson.put(
					"error",
					"Only " + createdCount + " of " + count +
						" companies were created.");
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create companies", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}
		finally {
			progressManager.finish();
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CompanyResourceCommand.class);

	@Reference
	private CompanyCreator _companyCreator;

	@Reference
	private Portal _portal;

}
