package com.liferay.support.tools.workflow.adapter.messageboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.MBThreadCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MBThreadCreateWorkflowAdapterTest {

	@Test
	void executeMapsCreatedThreadsIntoStepResult() throws Throwable {
		StubMBThreadCreator mbThreadCreator = new StubMBThreadCreator(
			List.of(
				_mbMessage(901L, 1001L, 1101L, 1201L, "Thread 1"),
				_mbMessage(902L, 1001L, 1101L, 1202L, "Thread 2")));

		MBThreadCreateWorkflowAdapter adapter =
			new MBThreadCreateWorkflowAdapter(mbThreadCreator);
		MBThreadCreateRequest request = new MBThreadCreateRequest(
			61L, 1001L, 1101L, new BatchSpec(2, "Thread"), "body", null);

		WorkflowStepResult<MBThreadStepItem> result = adapter.execute(
			request);

		assertEquals(
			MBThreadCreateWorkflowAdapter.OPERATION, result.operation());
		assertTrue(result.success());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				new MBThreadStepItem(1101L, 1001L, 901L, "Thread 1", 1201L),
				new MBThreadStepItem(1101L, 1001L, 902L, "Thread 2", 1202L)),
			result.items());
		assertEquals(61L, mbThreadCreator.userId);
		assertEquals(1001L, mbThreadCreator.groupId);
		assertEquals(1101L, mbThreadCreator.categoryId);
		assertEquals("body", mbThreadCreator.body);
		assertEquals("html", mbThreadCreator.format);
		assertSame(ProgressCallback.NOOP, mbThreadCreator.progressCallback);
	}

	@Test
	void requestRejectsNegativeCategoryId() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBThreadCreateRequest(
				61L, 1001L, -1L, new BatchSpec(1, "Thread"), "body", ""));
	}

	@Test
	void requestRejectsMissingBody() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBThreadCreateRequest(
				61L, 1001L, 1101L, new BatchSpec(1, "Thread"), null, ""));
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		MBThreadCreateWorkflowAdapter adapter =
			new MBThreadCreateWorkflowAdapter(
				new StubMBThreadCreator(
					List.of(_mbMessage(901L, 1001L, 1101L, 1201L, "Thread 1"))));

		WorkflowStepResult<MBThreadStepItem> result = adapter.execute(
			new MBThreadCreateRequest(
				61L, 1001L, 1101L, new BatchSpec(2, "Thread"), "body",
				"markdown"));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 MB threads were created.", result.error());
	}

	private static MBMessage _mbMessage(
		long messageId, long groupId, long categoryId, long threadId,
		String subject) {

		return TestModelProxyUtil.proxy(
			MBMessage.class,
			Map.of(
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getMessageId", messageId,
				"getSubject", subject,
				"getThreadId", threadId));
	}

	private static class StubMBThreadCreator extends MBThreadCreator {

		@Override
		public List<MBMessage> create(
			long userId, long groupId, long categoryId, BatchSpec batchSpec,
			String body, String format, ProgressCallback progress) {

			this.batchSpec = batchSpec;
			this.body = body;
			this.categoryId = categoryId;
			this.format = format;
			this.groupId = groupId;
			this.progressCallback = progress;
			this.userId = userId;

			return _messages;
		}

		private StubMBThreadCreator(List<MBMessage> messages) {
			_messages = messages;
		}

		private final List<MBMessage> _messages;
		private BatchSpec batchSpec;
		private String body;
		private long categoryId;
		private String format;
		private long groupId;
		private ProgressCallback progressCallback;
		private long userId;

	}

}
