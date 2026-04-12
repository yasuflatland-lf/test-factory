package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.service.LayoutCreator;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=layout.create",
	service = WorkflowOperationAdapter.class
)
public class LayoutCreateWorkflowOperationAdapter
	extends AbstractCreatorWorkflowOperationAdapter {

	public LayoutCreateWorkflowOperationAdapter() {
	}

	LayoutCreateWorkflowOperationAdapter(LayoutCreator layoutCreator) {
		_layoutCreator = layoutCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		LayoutCreateRequest request = LayoutCreateRequest.from(
			workflowExecutionContext, parameters(parameters));

		return normalizeStandardResult(
			_layoutCreator.create(
				request.userId(), request.batchSpec(), request.groupId(),
				request.type(), request.privateLayout(), request.hidden(),
				progressCallback()));
	}

	@Override
	public String operationName() {
		return "layout.create";
	}

	@Reference
	private LayoutCreator _layoutCreator;

}
