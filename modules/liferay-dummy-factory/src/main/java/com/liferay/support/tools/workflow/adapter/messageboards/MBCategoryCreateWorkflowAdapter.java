package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.support.tools.service.MBCategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class MBCategoryCreateWorkflowAdapter
	implements
		WorkflowOperationAdapter<MBCategoryCreateRequest, MBCategoryStepItem> {

	public static final String OPERATION = "mbCategory.create";

	public MBCategoryCreateWorkflowAdapter() {
	}

	MBCategoryCreateWorkflowAdapter(MBCategoryCreator mbCategoryCreator) {
		_mbCategoryCreator = mbCategoryCreator;
	}

	@Override
	public WorkflowStepResult<MBCategoryStepItem> execute(
			MBCategoryCreateRequest request)
		throws Throwable {

		List<MBCategory> categories = _mbCategoryCreator.create(
			request.userId(), request.groupId(), request.batch(),
			request.description(), ProgressCallback.NOOP);

		return WorkflowStepResult.fromItems(
			OPERATION, request.batch().count(), categories,
			MBCategoryStepItem::fromMBCategory, "MB categories");
	}

	@Override
	public String operation() {
		return OPERATION;
	}

	@Reference
	private MBCategoryCreator _mbCategoryCreator;

}
