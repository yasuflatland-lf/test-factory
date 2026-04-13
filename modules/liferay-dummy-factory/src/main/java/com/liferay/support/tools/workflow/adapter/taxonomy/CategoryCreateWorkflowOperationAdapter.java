package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.CategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.CategoryCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = "workflow.operation=category.create",
	service = WorkflowOperationAdapter.class
)
public class CategoryCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		CategoryCreateRequest request = new CategoryCreateRequest(
			workflowExecutionContext.userId(), values.requirePositiveLong("groupId"),
			values.requirePositiveLong("vocabularyId"),
			new BatchSpec(values.requireCount(), values.requireText("baseName")));

		List<AssetCategory> categories = _categoryCreator.create(
			request.userId(), request.groupId(), request.vocabularyId(),
			request.batch(), ProgressCallback.NOOP);

		return _toStepResult(
			request.batch().count(), categories, "categories");
	}

	@Override
	public String operationName() {
		return "category.create";
	}

	@Reference
	private CategoryCreator _categoryCreator;

	private static WorkflowStepResult _toStepResult(
		int requested, List<AssetCategory> categories, String itemName) {

		if (categories == null) {
			categories = List.of();
		}

		List<Map<String, Object>> items = new ArrayList<>(categories.size());

		for (AssetCategory category : categories) {
			Map<String, Object> item = new LinkedHashMap<>();

			item.put("categoryId", category.getCategoryId());
			item.put("groupId", category.getGroupId());
			item.put("vocabularyId", category.getVocabularyId());
			item.put("name", category.getName());

			items.add(item);
		}

		int createdCount = items.size();
		int skipped = Math.max(0, requested - createdCount);
		boolean success = (createdCount == requested);
		String error = success ? null :
			"Only " + createdCount + " of " + requested + " " + itemName +
				" were created.";

		return new WorkflowStepResult(
			success, requested, createdCount, skipped, items, error);
	}

}
