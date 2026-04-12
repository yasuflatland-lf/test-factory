package com.liferay.support.tools.portlet.actions;

import com.liferay.message.boards.model.MBMessage;
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
import com.liferay.support.tools.service.MBThreadCreator;
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
		"mvc.command.name=/ldf/mb-thread"
	},
	service = MVCResourceCommand.class
)
public class MBThreadResourceCommand extends BaseMVCResourceCommand {

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

			BatchSpec batchSpec = ResourceCommandUtil.parseBatchSpec(data);

			long groupId = GetterUtil.getLong(data.getString("groupId"));
			long categoryId = GetterUtil.getLong(data.getString("categoryId"));

			String body = GetterUtil.getString(
				data.getString("body"), "This is a test message.");
			String format = GetterUtil.getString(
				data.getString("format"), "html");

			ResourceCommandUtil.validatePositiveId(groupId, "groupId");

			if (categoryId < 0) {
				throw new IllegalArgumentException(
					"categoryId must be greater than or equal to 0");
			}

			long userId = _portal.getUserId(resourceRequest);

			List<MBMessage> messages = _mbThreadCreator.create(
				userId, groupId, categoryId, batchSpec, body, format,
				(current, total) -> progressManager.trackProgress(
					current, total));

			JSONArray itemsArray = JSONFactoryUtil.createJSONArray();

			for (MBMessage message : messages) {
				JSONObject threadJson = JSONFactoryUtil.createJSONObject();

				threadJson.put("messageId", message.getMessageId());
				threadJson.put("subject", message.getSubject());
				threadJson.put("threadId", message.getThreadId());

				itemsArray.put(threadJson);
			}

			int requested = batchSpec.count();
			int created = messages.size();
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
						" MB threads were created.");
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create MB threads", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}
		finally {
			progressManager.finish();
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MBThreadResourceCommand.class);

	@Reference
	private MBThreadCreator _mbThreadCreator;

	@Reference
	private Portal _portal;

}
