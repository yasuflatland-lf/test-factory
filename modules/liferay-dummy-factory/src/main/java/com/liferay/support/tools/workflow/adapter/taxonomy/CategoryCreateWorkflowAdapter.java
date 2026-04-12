package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.support.tools.service.CategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class CategoryCreateWorkflowAdapter
	implements WorkflowOperationAdapter<CategoryCreateRequest, CategoryStepItem> {

	public static final String OPERATION = "category.create";

	public CategoryCreateWorkflowAdapter() {
	}

	CategoryCreateWorkflowAdapter(CategoryCreator categoryCreator) {
		_categoryCreator = categoryCreator;
	}

	@Override
	public WorkflowStepResult<CategoryStepItem> execute(
			CategoryCreateRequest request)
		throws Throwable {

		List<AssetCategory> categories = _categoryCreator.create(
			request.userId(), request.groupId(), request.vocabularyId(),
			request.batch(), ProgressCallback.NOOP);

		return WorkflowStepResult.fromItems(
			OPERATION, request.batch().count(), categories,
			CategoryStepItem::fromAssetCategory, "categories");
	}

	@Override
	public String operation() {
		return OPERATION;
	}

	@Reference
	private CategoryCreator _categoryCreator;

}
