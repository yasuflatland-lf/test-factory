package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.MBReplyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class MBReplyCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	public MBReplyCreateWorkflowOperationAdapter() {
	}

	MBReplyCreateWorkflowOperationAdapter(MBReplyCreator mbReplyCreator) {
		_mbReplyCreator = mbReplyCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		long userId = workflowExecutionContext.userId();

		// Honor explicit per-step override if present; schema documents `userId`
		// as an optional parameter for impersonation-like workflows.
		if ((parameters != null) && parameters.containsKey("userId")) {
			long overrideUserId = values.optionalLong("userId", Long.MIN_VALUE);

			if (overrideUserId != Long.MIN_VALUE) {
				if (overrideUserId <= 0) {
					throw new IllegalArgumentException(
						"userId must be positive");
				}

				userId = overrideUserId;
			}
		}

		MBReplyCreateRequest request = new MBReplyCreateRequest(
			userId, values.requirePositiveLong("threadId"),
			values.requireCount(), values.requireText("body"),
			values.optionalString("format", "html"));

		List<MBMessage> replies = _mbReplyCreator.create(
			request.userId(), request.threadId(), request.count(), request.body(),
			request.format(), ProgressCallback.NOOP);

		return _toStepResult(request.count(), replies, "MB replies");
	}

	@Override
	public String operationName() {
		return "mbReply.create";
	}

	@Reference
	private MBReplyCreator _mbReplyCreator;

	private static WorkflowStepResult _toStepResult(
		int requested, List<MBMessage> replies, String itemName) {

		List<Map<String, Object>> items = new ArrayList<>(replies.size());

		for (MBMessage reply : replies) {
			Map<String, Object> item = new LinkedHashMap<>();

			item.put("body", reply.getBody());
			item.put("messageId", reply.getMessageId());
			item.put("subject", reply.getSubject());

			items.add(item);
		}

		int createdCount = items.size();
		int skipped = requested - createdCount;
		boolean success = (createdCount == requested);
		String error = success ? null :
			"Only " + createdCount + " of " + requested + " " + itemName +
				" were created.";

		return new WorkflowStepResult(
			success, requested, createdCount, skipped, items, error);
	}

}
