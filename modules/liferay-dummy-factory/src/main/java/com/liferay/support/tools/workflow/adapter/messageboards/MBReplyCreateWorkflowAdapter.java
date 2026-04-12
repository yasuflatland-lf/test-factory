package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.MBReplyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class MBReplyCreateWorkflowAdapter
	implements WorkflowOperationAdapter<MBReplyCreateRequest, MBReplyStepItem> {

	public static final String OPERATION = "mbReply.create";

	public MBReplyCreateWorkflowAdapter() {
	}

	MBReplyCreateWorkflowAdapter(MBReplyCreator mbReplyCreator) {
		_mbReplyCreator = mbReplyCreator;
	}

	@Override
	public WorkflowStepResult<MBReplyStepItem> execute(
			MBReplyCreateRequest request)
		throws Throwable {

		List<MBMessage> replies = _mbReplyCreator.create(
			request.userId(), request.threadId(), request.count(),
			request.body(), request.format(), ProgressCallback.NOOP);

		return WorkflowStepResult.fromItems(
			OPERATION, request.count(), replies,
			MBReplyStepItem::fromMBMessage, "MB replies");
	}

	@Override
	public String operation() {
		return OPERATION;
	}

	@Reference
	private MBReplyCreator _mbReplyCreator;

}
