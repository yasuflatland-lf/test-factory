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
import com.liferay.support.tools.service.RoleCreator;
import com.liferay.support.tools.service.RoleType;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.utils.ProgressManager;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
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

		ProgressManager progressManager = new ProgressManager();

		boolean progressStarted = false;

		try {
			progressManager.start(resourceRequest);
			progressStarted = true;

			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

			RoleType roleType = RoleType.fromString(
				GetterUtil.getString(data.getString("roleType"), "regular"));
			String description = GetterUtil.getString(
				data.getString("description"), "");

			long userId = _portal.getUserId(resourceRequest);

			responseJson = _roleCreator.create(
				userId, batchSpec, roleType, description,
				ProgressCallback.fromProgressManager(
					progressManager));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create roles", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}
		finally {
			if (progressStarted) {
				progressManager.finish();
			}
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
