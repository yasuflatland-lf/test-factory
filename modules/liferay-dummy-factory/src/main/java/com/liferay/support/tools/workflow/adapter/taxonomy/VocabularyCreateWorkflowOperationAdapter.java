package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.VocabularyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.VocabularyCreateRequest;
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
	property = "workflow.operation=vocabulary.create",
	service = WorkflowOperationAdapter.class
)
public class VocabularyCreateWorkflowOperationAdapter
	implements WorkflowOperationAdapter {

	VocabularyCreateWorkflowOperationAdapter() {
	}

	VocabularyCreateWorkflowOperationAdapter(
		VocabularyCreator vocabularyCreator) {

		_vocabularyCreator = vocabularyCreator;
	}

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		VocabularyCreateRequest request = new VocabularyCreateRequest(
			workflowExecutionContext.userId(), values.requirePositiveLong("groupId"),
			new BatchSpec(values.requireCount(), values.requireText("baseName")));

		List<AssetVocabulary> vocabularies = _vocabularyCreator.create(
			request.userId(), request.groupId(), request.batch(),
			ProgressCallback.NOOP);

		return _toStepResult(
			request.batch().count(), vocabularies, "vocabularies");
	}

	@Override
	public String operationName() {
		return "vocabulary.create";
	}

	@Reference
	private VocabularyCreator _vocabularyCreator;

	private static WorkflowStepResult _toStepResult(
		int requested, List<AssetVocabulary> vocabularies, String itemName) {

		if (vocabularies == null) {
			vocabularies = List.of();
		}

		List<Map<String, Object>> items = new ArrayList<>(vocabularies.size());

		for (AssetVocabulary vocabulary : vocabularies) {
			Map<String, Object> item = new LinkedHashMap<>();

			item.put("vocabularyId", vocabulary.getVocabularyId());
			item.put("groupId", vocabulary.getGroupId());
			item.put("name", vocabulary.getName());

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
