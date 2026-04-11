package com.liferay.support.tools.service;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.model.MBThread;
import com.liferay.message.boards.service.MBMessageLocalService;
import com.liferay.message.boards.service.MBThreadLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MBReplyCreator.class)
public class MBReplyCreator {

	public List<MBMessage> create(
			long userId, long threadId, int count, String body, String format)
		throws Throwable {

		MBThread thread = _mbThreadLocalService.getMBThread(threadId);

		long groupId = thread.getGroupId();
		long categoryId = thread.getCategoryId();
		long rootMessageId = thread.getRootMessageId();

		String userName = _userLocalService.getUser(userId).getFullName();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		List<MBMessage> replies = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String subject = "RE: reply " + (i + 1);

			replies.add(
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> _mbMessageLocalService.addMessage(
						null, userId, userName, groupId, categoryId, threadId,
						rootMessageId, subject, body, format,
						Collections.emptyList(), false, 0.0, false,
						serviceContext)));
		}

		return replies;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private MBMessageLocalService _mbMessageLocalService;

	@Reference
	private MBThreadLocalService _mbThreadLocalService;

	@Reference
	private UserLocalService _userLocalService;

}
