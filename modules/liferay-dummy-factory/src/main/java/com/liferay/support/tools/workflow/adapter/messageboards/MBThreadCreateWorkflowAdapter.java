package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.MBThreadCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class MBThreadCreateWorkflowAdapter
	implements WorkflowOperationAdapter<MBThreadCreateRequest, MBThreadStepItem> {

	public static final String OPERATION = "mbThread.create";

	public MBThreadCreateWorkflowAdapter() {
	}

	MBThreadCreateWorkflowAdapter(MBThreadCreator mbThreadCreator) {
		_mbThreadCreator = mbThreadCreator;
	}

	@Override
	public WorkflowStepResult<MBThreadStepItem> execute(
			MBThreadCreateRequest request)
		throws Throwable {

		List<MBMessage> messages = _mbThreadCreator.create(
			request.userId(), request.groupId(), request.categoryId(),
			request.batch(), request.body(), request.format(),
			ProgressCallback.NOOP);

		return WorkflowStepResult.fromItems(
			OPERATION, request.batch().count(), messages,
			MBThreadStepItem::fromMBMessage, "MB threads");
	}

	@Override
	public String operation() {
		return OPERATION;
	}

	@Reference
	private MBThreadCreator _mbThreadCreator;

}
