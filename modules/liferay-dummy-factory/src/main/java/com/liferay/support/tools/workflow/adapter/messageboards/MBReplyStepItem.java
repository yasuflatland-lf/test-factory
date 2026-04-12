package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;

public record MBReplyStepItem(
	String body, long categoryId, long groupId, long messageId, String subject,
	long threadId) {

	public static MBReplyStepItem fromMBMessage(MBMessage message) {
		return new MBReplyStepItem(
			message.getBody(), message.getCategoryId(), message.getGroupId(),
			message.getMessageId(), message.getSubject(),
			message.getThreadId());
	}

}
