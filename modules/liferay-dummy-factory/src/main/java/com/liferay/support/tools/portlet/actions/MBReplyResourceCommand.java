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
import com.liferay.support.tools.service.MBReplyCreator;
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
		"mvc.command.name=/ldf/mb-reply"
	},
	service = MVCResourceCommand.class
)
public class MBReplyResourceCommand extends BaseMVCResourceCommand {

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
			long threadId = GetterUtil.getLong(data.getString("threadId"));

			String body = GetterUtil.getString(
				data.getString("body"), "This is a test reply.");
			String format = GetterUtil.getString(
				data.getString("format"), "html");

			ResourceCommandUtil.validateCount(count);

			ResourceCommandUtil.validatePositiveId(threadId, "threadId");

			long userId = _portal.getUserId(resourceRequest);

			List<MBMessage> replies = _mbReplyCreator.create(
				userId, threadId, count, body, format,
				(current, total) -> progressManager.trackProgress(current, total));

			JSONArray itemsArray = JSONFactoryUtil.createJSONArray();

			for (MBMessage reply : replies) {
				JSONObject replyJson = JSONFactoryUtil.createJSONObject();

				replyJson.put("body", reply.getBody());
				replyJson.put("messageId", reply.getMessageId());
				replyJson.put("subject", reply.getSubject());

				itemsArray.put(replyJson);
			}

			int createdCount = replies.size();
			boolean success = (createdCount == count);

			responseJson.put("count", createdCount);
			responseJson.put("items", itemsArray);
			responseJson.put("requested", count);
			responseJson.put("skipped", 0);
			responseJson.put("success", success);

			if (!success) {
				responseJson.put(
					"error",
					"Only " + createdCount + " of " + count +
						" MB replies were created.");
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			ResourceCommandUtil.setErrorResponse(
				responseJson, illegalArgumentException);
		}
		catch (Throwable throwable) {
			_log.error("Failed to create MB replies", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}
		finally {
			progressManager.finish();
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MBReplyResourceCommand.class);

	@Reference
	private MBReplyCreator _mbReplyCreator;

	@Reference
	private Portal _portal;

}
