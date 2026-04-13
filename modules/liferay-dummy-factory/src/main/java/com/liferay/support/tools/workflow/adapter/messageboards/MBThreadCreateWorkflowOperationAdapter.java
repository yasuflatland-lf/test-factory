package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBThreadCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=mbThread.create",
	service = WorkflowOperationAdapter.class
)
public class MBThreadCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	public static final String OPERATION = "mbThread.create";

	public MBThreadCreateWorkflowOperationAdapter() {
	}

	MBThreadCreateWorkflowOperationAdapter(MBThreadCreator mbThreadCreator) {
		_mbThreadCreator = mbThreadCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		MBThreadCreateRequest request = new MBThreadCreateRequest(
			_effectiveUserId(values, workflowExecutionContext),
			values.requirePositiveLong("groupId"),
			values.optionalLong("categoryId", 0L), _batchSpec(values),
			values.requireText("body"), values.optionalString("format", "html"));

		List<MBMessage> messages = _mbThreadCreator.create(
			request.userId(), request.groupId(), request.categoryId(),
			request.batch(), request.body(), request.format(),
			ProgressCallback.NOOP);

		List<Map<String, Object>> items = messages.stream(
		).map(
			MBThreadStepItem::fromMBMessage
		).map(
			MBThreadCreateWorkflowOperationAdapter::_toItem
		).toList();

		return _toStepResult(request.batch().count(), items, "MB threads");
	}

	@Override
	public String operationName() {
		return OPERATION;
	}

	private static BatchSpec _batchSpec(WorkflowParameterValues values) {
		return new BatchSpec(values.requireCount(), values.requireText("baseName"));
	}

	private static long _effectiveUserId(
		WorkflowParameterValues values,
		WorkflowExecutionContext workflowExecutionContext) {

		long userId = values.optionalLong("userId", workflowExecutionContext.userId());

		if (userId <= 0) {
			throw new IllegalArgumentException("userId is required");
		}

		return userId;
	}

	private static Map<String, Object> _toItem(MBThreadStepItem stepItem) {
		Map<String, Object> item = new LinkedHashMap<>();

		item.put("categoryId", stepItem.categoryId());
		item.put("groupId", stepItem.groupId());
		item.put("messageId", stepItem.messageId());
		item.put("subject", stepItem.subject());
		item.put("threadId", stepItem.threadId());

		return item;
	}

	private static WorkflowStepResult _toStepResult(
		int requested, List<Map<String, Object>> items, String noun) {

		int count = items.size();
		int skipped = Math.max(0, requested - count);
		boolean success = (count == requested);
		String error = null;

		if (!success) {
			error =
				"Only " + count + " of " + requested + " " + noun +
					" were created.";
		}

		return new WorkflowStepResult(
			success, requested, count, skipped, items, error);
	}

	@Reference
	private MBThreadCreator _mbThreadCreator;

}
