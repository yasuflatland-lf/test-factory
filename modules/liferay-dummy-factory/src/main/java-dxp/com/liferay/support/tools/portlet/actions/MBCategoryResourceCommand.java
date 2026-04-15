package com.liferay.support.tools.portlet.actions;

import com.liferay.message.boards.model.MBCategory;
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
import com.liferay.support.tools.service.MBCategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.List;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"jakarta.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/mb-category"
	},
	service = MVCResourceCommand.class
)
public class MBCategoryResourceCommand extends BaseMVCResourceCommand {

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

			ResourceCommandUtil.validatePositiveId(groupId, "groupId");

			String description = GetterUtil.getString(
				data.getString("description"));

			long userId = _portal.getUserId(resourceRequest);

			List<MBCategory> categories = _mbCategoryCreator.create(
				userId, groupId, batchSpec, description,
				ProgressCallback.fromProgressManager(
					progressManager));

			JSONArray itemsArray = JSONFactoryUtil.createJSONArray();

			for (MBCategory category : categories) {
				JSONObject categoryJson = JSONFactoryUtil.createJSONObject();

				categoryJson.put("categoryId", category.getCategoryId());
				categoryJson.put("name", category.getName());

				itemsArray.put(categoryJson);
			}

			int requested = batchSpec.count();
			int created = categories.size();
			boolean success = (created == requested);

			responseJson.put("count", created);
			responseJson.put("items", itemsArray);
			responseJson.put("requested", requested);
			responseJson.put("skipped", 0);
			responseJson.put("success", success);

			if (!success) {
				responseJson.put(
					"error",
					"Only " + created + " of " + requested +
						" MB categories were created.");
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create MB categories", throwable);

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
		MBCategoryResourceCommand.class);

	@Reference
	private MBCategoryCreator _mbCategoryCreator;

	@Reference
	private Portal _portal;

}
