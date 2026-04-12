package com.liferay.support.tools.workflow.adapter.taxonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.VocabularyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VocabularyCreateWorkflowAdapterTest {

	@Test
	void executeMapsCreatedVocabulariesIntoStepResult() throws Throwable {
		StubVocabularyCreator vocabularyCreator = new StubVocabularyCreator(
			List.of(
				_assetVocabulary(501L, 601L, "Vocabulary 1"),
				_assetVocabulary(502L, 601L, "Vocabulary 2")));

		VocabularyCreateWorkflowAdapter adapter =
			new VocabularyCreateWorkflowAdapter(vocabularyCreator);
		VocabularyCreateRequest request = new VocabularyCreateRequest(
			41L, 601L, new BatchSpec(2, "Vocabulary"));

		WorkflowStepResult<VocabularyStepItem> result = adapter.execute(
			request);

		assertEquals(
			VocabularyCreateWorkflowAdapter.OPERATION, result.operation());
		assertTrue(result.success());
		assertEquals(2, result.requested());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				new VocabularyStepItem(501L, 601L, "Vocabulary 1"),
				new VocabularyStepItem(502L, 601L, "Vocabulary 2")),
			result.items());
		assertEquals(41L, vocabularyCreator.userId);
		assertEquals(601L, vocabularyCreator.groupId);
		assertEquals(2, vocabularyCreator.batchSpec.count());
		assertEquals(
			"Vocabulary", vocabularyCreator.batchSpec.baseName());
		assertSame(ProgressCallback.NOOP, vocabularyCreator.progressCallback);
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		VocabularyCreateWorkflowAdapter adapter =
			new VocabularyCreateWorkflowAdapter(
				new StubVocabularyCreator(
					List.of(_assetVocabulary(501L, 601L, "Vocabulary 1"))));

		WorkflowStepResult<VocabularyStepItem> result = adapter.execute(
			new VocabularyCreateRequest(
				41L, 601L, new BatchSpec(2, "Vocabulary")));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 vocabularies were created.", result.error());
	}

	@Test
	void requestRejectsInvalidGroupId() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new VocabularyCreateRequest(
				41L, 0L, new BatchSpec(1, "Vocabulary")));
	}

	private static AssetVocabulary _assetVocabulary(
		long vocabularyId, long groupId, String name) {

		return TestModelProxyUtil.proxy(
			AssetVocabulary.class,
			Map.of(
				"getGroupId", groupId,
				"getName", name,
				"getVocabularyId", vocabularyId));
	}

	private static class StubVocabularyCreator extends VocabularyCreator {

		@Override
		public List<AssetVocabulary> create(
			long userId, long groupId, BatchSpec batchSpec,
			ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.groupId = groupId;
			this.progressCallback = progress;
			this.userId = userId;

			return _vocabularies;
		}

		private StubVocabularyCreator(List<AssetVocabulary> vocabularies) {
			_vocabularies = vocabularies;
		}

		private final List<AssetVocabulary> _vocabularies;
		private BatchSpec batchSpec;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;

	}

}
