package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;

public record MBThreadStepItem(
	long categoryId, long groupId, long messageId, String subject,
	long threadId) {

	public static MBThreadStepItem fromMBMessage(MBMessage message) {
		return new MBThreadStepItem(
			message.getCategoryId(), message.getGroupId(),
			message.getMessageId(), message.getSubject(),
			message.getThreadId());
	}

}
