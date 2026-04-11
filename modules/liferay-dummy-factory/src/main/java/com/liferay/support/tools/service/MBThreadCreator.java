package com.liferay.support.tools.service;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.service.MBMessageLocalService;
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

@Component(service = MBThreadCreator.class)
public class MBThreadCreator {

	public List<MBMessage> create(
			long userId, long groupId, long categoryId, BatchSpec batchSpec,
			String body, String format)
		throws Throwable {

		int count = batchSpec.count();

		String userName = _userLocalService.getUser(userId).getFullName();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		List<MBMessage> messages = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String subject = BatchNaming.resolve(
				batchSpec.baseName(), count, i, " ");

			messages.add(
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> _mbMessageLocalService.addMessage(
						null, userId, userName, groupId, categoryId, 0L, 0L,
						subject, body, format, Collections.emptyList(), false,
						0.0, false, serviceContext)));
		}

		return messages;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private MBMessageLocalService _mbMessageLocalService;

	@Reference
	private UserLocalService _userLocalService;

}
