package com.liferay.support.tools.workflow.adapter.taxonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.CategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CategoryCreateWorkflowAdapterTest {

	@Test
	void executeMapsCreatedCategoriesIntoStepResult() throws Throwable {
		StubCategoryCreator categoryCreator = new StubCategoryCreator(
			List.of(
				_assetCategory(101L, 201L, 301L, "Category 1"),
				_assetCategory(102L, 201L, 301L, "Category 2")));

		CategoryCreateWorkflowAdapter adapter =
			new CategoryCreateWorkflowAdapter(categoryCreator);
		CategoryCreateRequest request = new CategoryCreateRequest(
			11L, 201L, 301L, new BatchSpec(2, "Category"));

		WorkflowStepResult<CategoryStepItem> result = adapter.execute(request);

		assertEquals(CategoryCreateWorkflowAdapter.OPERATION, result.operation());
		assertTrue(result.success());
		assertEquals(2, result.requested());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				new CategoryStepItem(101L, 201L, 301L, "Category 1"),
				new CategoryStepItem(102L, 201L, 301L, "Category 2")),
			result.items());
		assertEquals(11L, categoryCreator.userId);
		assertEquals(201L, categoryCreator.groupId);
		assertEquals(301L, categoryCreator.vocabularyId);
		assertEquals(2, categoryCreator.batchSpec.count());
		assertEquals("Category", categoryCreator.batchSpec.baseName());
		assertSame(ProgressCallback.NOOP, categoryCreator.progressCallback);
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		CategoryCreateWorkflowAdapter adapter =
			new CategoryCreateWorkflowAdapter(
				new StubCategoryCreator(
					List.of(_assetCategory(101L, 201L, 301L, "Category 1"))));

		WorkflowStepResult<CategoryStepItem> result = adapter.execute(
			new CategoryCreateRequest(
				11L, 201L, 301L, new BatchSpec(2, "Category")));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 categories were created.", result.error());
	}

	@Test
	void requestRejectsInvalidVocabularyId() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new CategoryCreateRequest(
				11L, 201L, 0L, new BatchSpec(1, "Category")));
	}

	private static AssetCategory _assetCategory(
		long categoryId, long groupId, long vocabularyId, String name) {

		return TestModelProxyUtil.proxy(
			AssetCategory.class,
			Map.of(
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getName", name,
				"getVocabularyId", vocabularyId));
	}

	private static class StubCategoryCreator extends CategoryCreator {

		@Override
		public List<AssetCategory> create(
			long userId, long groupId, long vocabularyId, BatchSpec batchSpec,
			ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.groupId = groupId;
			this.progressCallback = progress;
			this.userId = userId;
			this.vocabularyId = vocabularyId;

			return _categories;
		}

		private StubCategoryCreator(List<AssetCategory> categories) {
			_categories = categories;
		}

		private final List<AssetCategory> _categories;
		private BatchSpec batchSpec;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;
		private long vocabularyId;

	}

}
