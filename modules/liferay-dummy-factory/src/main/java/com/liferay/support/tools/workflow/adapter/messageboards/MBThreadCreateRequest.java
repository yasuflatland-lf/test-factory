package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.workflow.adapter.WorkflowInputValidator;

import java.util.Objects;

public record MBThreadCreateRequest(
	long userId, long groupId, long categoryId, BatchSpec batch, String body,
	String format) {

	public MBThreadCreateRequest {
		userId = WorkflowInputValidator.requirePositiveId(userId, "userId");
		groupId = WorkflowInputValidator.requirePositiveId(groupId, "groupId");
		categoryId = WorkflowInputValidator.requireNonNegativeId(
			categoryId, "categoryId");
		batch = Objects.requireNonNull(batch, "batch is required");
		body = WorkflowInputValidator.normalizeText(
			body, "This is a test message.");
		format = WorkflowInputValidator.normalizeText(format, "html");
	}

}
