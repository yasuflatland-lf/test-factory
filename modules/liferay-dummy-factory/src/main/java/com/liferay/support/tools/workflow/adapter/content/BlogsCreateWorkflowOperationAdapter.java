package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.service.BlogsCreator;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=blogs.create",
	service = WorkflowOperationAdapter.class
)
public class BlogsCreateWorkflowOperationAdapter
	extends AbstractCreatorWorkflowOperationAdapter {

	public BlogsCreateWorkflowOperationAdapter() {
	}

	BlogsCreateWorkflowOperationAdapter(BlogsCreator blogsCreator) {
		_blogsCreator = blogsCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		BlogsCreateRequest request = BlogsCreateRequest.from(
			workflowExecutionContext, parameters(parameters));

		return normalizeStandardResult(
			_blogsCreator.create(
				request.userId(), request.batchSpec(), progressCallback()));
	}

	@Override
	public String operationName() {
		return "blogs.create";
	}

	@Reference
	private BlogsCreator _blogsCreator;

}
