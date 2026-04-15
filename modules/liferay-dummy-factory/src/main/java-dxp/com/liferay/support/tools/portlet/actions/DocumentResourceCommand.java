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
import com.liferay.portal.kernel.util.Validator;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.DocumentCreator;
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
		"mvc.command.name=/ldf/doc"
	},
	service = MVCResourceCommand.class
)
public class DocumentResourceCommand extends BaseMVCResourceCommand {

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

		boolean progressStarted = false;

		try {
			progressManager.start(resourceRequest);
			progressStarted = true;

			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

			long groupId = GetterUtil.getLong(data.getString("groupId"));

			if (groupId <= 0) {
				throw new IllegalArgumentException("site is required");
			}

			long folderId = GetterUtil.getLong(
				data.getString("folderId"), 0L);
			String description = GetterUtil.getString(
				data.getString("description"), "");
			String uploadedFilesStr = data.getString("uploadedFiles");

			String[] uploadedFiles = Validator.isNotNull(uploadedFilesStr) ?
				uploadedFilesStr.split(",") : new String[0];

			long userId = _portal.getUserId(resourceRequest);

			responseJson = _documentCreator.create(
				userId, groupId, batchSpec, folderId, description,
				uploadedFiles,
				ProgressCallback.fromProgressManager(
					progressManager));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create documents", throwable);

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
		DocumentResourceCommand.class);

	@Reference
	private DocumentCreator _documentCreator;

	@Reference
	private Portal _portal;

}
