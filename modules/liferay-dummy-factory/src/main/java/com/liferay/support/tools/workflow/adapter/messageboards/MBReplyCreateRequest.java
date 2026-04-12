package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.support.tools.workflow.adapter.WorkflowInputValidator;

public record MBReplyCreateRequest(
	long userId, long threadId, int count, String body, String format) {

	public MBReplyCreateRequest {
		userId = WorkflowInputValidator.requirePositiveId(userId, "userId");
		threadId = WorkflowInputValidator.requirePositiveId(
			threadId, "threadId");
		count = WorkflowInputValidator.requireCount(count);
		body = WorkflowInputValidator.normalizeText(
			body, "This is a test reply.");
		format = WorkflowInputValidator.normalizeText(format, "html");
	}

}
