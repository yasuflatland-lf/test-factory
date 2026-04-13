package com.liferay.support.tools.workflow.adapter.messageboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.message.boards.model.MBCategory;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBCategoryCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MBCategoryCreateWorkflowAdapterTest {

	@Test
	void executeMapsCreatedCategoriesIntoStepResult() throws Throwable {
		StubMBCategoryCreator mbCategoryCreator = new StubMBCategoryCreator(
			List.of(
				_mbCategory(701L, 801L, "MB Category 1"),
				_mbCategory(702L, 801L, "MB Category 2")));

		MBCategoryCreateWorkflowAdapter adapter =
			new MBCategoryCreateWorkflowAdapter(mbCategoryCreator);
		MBCategoryCreateRequest request = new MBCategoryCreateRequest(
			51L, 801L, new BatchSpec(2, "MB Category"), "desc");

		WorkflowStepResult<MBCategoryStepItem> result = adapter.execute(
			request);

		assertEquals(
			MBCategoryCreateWorkflowAdapter.OPERATION, result.operation());
		assertTrue(result.success());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				new MBCategoryStepItem(701L, 801L, "MB Category 1"),
				new MBCategoryStepItem(702L, 801L, "MB Category 2")),
			result.items());
		assertEquals(51L, mbCategoryCreator.userId);
		assertEquals(801L, mbCategoryCreator.groupId);
		assertEquals("desc", mbCategoryCreator.description);
		assertSame(ProgressCallback.NOOP, mbCategoryCreator.progressCallback);
	}

	@Test
	void requestRejectsMissingDescription() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBCategoryCreateRequest(
				51L, 801L, new BatchSpec(1, "MB Category"), null));
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		MBCategoryCreateWorkflowAdapter adapter =
			new MBCategoryCreateWorkflowAdapter(
				new StubMBCategoryCreator(
					List.of(_mbCategory(701L, 801L, "MB Category 1"))));

		WorkflowStepResult<MBCategoryStepItem> result = adapter.execute(
			new MBCategoryCreateRequest(
				51L, 801L, new BatchSpec(2, "MB Category"), "desc"));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 MB categories were created.", result.error());
	}

	private static MBCategory _mbCategory(
		long categoryId, long groupId, String name) {

		return TestModelProxyUtil.proxy(
			MBCategory.class,
			Map.of(
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getName", name));
	}

	private static class StubMBCategoryCreator extends MBCategoryCreator {

		@Override
		public List<MBCategory> create(
			long userId, long groupId, BatchSpec batchSpec, String description,
			ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.description = description;
			this.groupId = groupId;
			this.progressCallback = progress;
			this.userId = userId;

			return _categories;
		}

		private StubMBCategoryCreator(List<MBCategory> categories) {
			_categories = categories;
		}

		private final List<MBCategory> _categories;
		private BatchSpec batchSpec;
		private String description;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;

	}

}
