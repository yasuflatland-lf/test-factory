package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.support.tools.service.VocabularyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.VocabularyCreateRequest;
import com.liferay.support.tools.workflow.adapter.taxonomy.dto.VocabularyStepItem;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class VocabularyCreateWorkflowAdapter
	implements
		WorkflowOperationAdapter
			<VocabularyCreateRequest, VocabularyStepItem> {

	public static final String OPERATION = "vocabulary.create";

	public VocabularyCreateWorkflowAdapter() {
	}

	VocabularyCreateWorkflowAdapter(VocabularyCreator vocabularyCreator) {
		_vocabularyCreator = vocabularyCreator;
	}

	@Override
	public WorkflowStepResult<VocabularyStepItem> execute(
			VocabularyCreateRequest request)
		throws Throwable {

		List<AssetVocabulary> vocabularies = _vocabularyCreator.create(
			request.userId(), request.groupId(), request.batch(),
			ProgressCallback.NOOP);

		return WorkflowStepResult.fromItems(
			OPERATION, request.batch().count(), vocabularies,
			VocabularyStepItem::fromAssetVocabulary, "vocabularies");
	}

	@Override
	public String operation() {
		return OPERATION;
	}

	@Reference
	private VocabularyCreator _vocabularyCreator;

}
