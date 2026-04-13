package com.liferay.support.tools.workflow.adapter.content;

import com.liferay.support.tools.service.DocumentCreator;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=document.create",
	service = WorkflowOperationAdapter.class
)
public class DocumentCreateWorkflowOperationAdapter
	extends AbstractCreatorWorkflowOperationAdapter {

	public DocumentCreateWorkflowOperationAdapter() {
	}

	DocumentCreateWorkflowOperationAdapter(DocumentCreator documentCreator) {
		_documentCreator = documentCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		DocumentCreateRequest request = DocumentCreateRequest.from(
			workflowExecutionContext, parameters(parameters));

		return normalizeStandardResult(
			_documentCreator.create(
				request.userId(), request.groupId(), request.batchSpec(),
				request.folderId(), request.description(),
				request.uploadedFiles(), progressCallback()));
	}

	@Override
	public String operationName() {
		return "document.create";
	}

	@Reference
	private DocumentCreator _documentCreator;

}
