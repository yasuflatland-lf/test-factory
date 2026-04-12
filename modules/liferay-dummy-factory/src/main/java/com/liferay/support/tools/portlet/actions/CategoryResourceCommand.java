package com.liferay.support.tools.portlet.actions;

import com.liferay.asset.kernel.model.AssetCategory;
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
import com.liferay.support.tools.service.CategoryCreator;
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
		"mvc.command.name=/ldf/category"
	},
	service = MVCResourceCommand.class
)
public class CategoryResourceCommand extends BaseMVCResourceCommand {

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

		progressManager.start(resourceRequest);

		try {
			JSONObject data = JSONFactoryUtil.createJSONObject(dataString);

			BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

			long groupId = GetterUtil.getLong(data.getString("groupId"));
			long vocabularyId = GetterUtil.getLong(
				data.getString("vocabularyId"));

			if (groupId <= 0) {
				throw new IllegalArgumentException(
					"groupId must be greater than 0");
			}

			if (vocabularyId <= 0) {
				throw new IllegalArgumentException(
					"vocabularyId is required");
			}

			long userId = _portal.getUserId(resourceRequest);

			List<AssetCategory> categories = _categoryCreator.create(
				userId, groupId, vocabularyId, batchSpec,
				(current, total) -> progressManager.trackProgress(
					current, total));

			JSONArray itemsArray = JSONFactoryUtil.createJSONArray();

			for (AssetCategory category : categories) {
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
						" categories were created.");
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create categories", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}
		finally {
			progressManager.finish();
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CategoryResourceCommand.class);

	@Reference
	private CategoryCreator _categoryCreator;

	@Reference
	private Portal _portal;

}
