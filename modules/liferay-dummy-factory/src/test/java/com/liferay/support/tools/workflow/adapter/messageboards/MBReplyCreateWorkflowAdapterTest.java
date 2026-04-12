package com.liferay.support.tools.workflow.adapter.messageboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.support.tools.service.MBReplyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.adapter.WorkflowStepResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MBReplyCreateWorkflowAdapterTest {

	@Test
	void executeMapsCreatedRepliesIntoStepResult() throws Throwable {
		StubMBReplyCreator mbReplyCreator = new StubMBReplyCreator(
			List.of(
				_mbReply(1301L, 1401L, 1501L, 1601L, "Reply 1", "Body 1"),
				_mbReply(1302L, 1401L, 1501L, 1601L, "Reply 2", "Body 2")));

		MBReplyCreateWorkflowAdapter adapter =
			new MBReplyCreateWorkflowAdapter(mbReplyCreator);
		MBReplyCreateRequest request = new MBReplyCreateRequest(
			71L, 1601L, 2, null, null);

		WorkflowStepResult<MBReplyStepItem> result = adapter.execute(request);

		assertEquals(
			MBReplyCreateWorkflowAdapter.OPERATION, result.operation());
		assertTrue(result.success());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertNull(result.error());
		assertEquals(
			List.of(
				new MBReplyStepItem(
					"Body 1", 1501L, 1401L, 1301L, "Reply 1", 1601L),
				new MBReplyStepItem(
					"Body 2", 1501L, 1401L, 1302L, "Reply 2", 1601L)),
			result.items());
		assertEquals(71L, mbReplyCreator.userId);
		assertEquals(1601L, mbReplyCreator.threadId);
		assertEquals("This is a test reply.", mbReplyCreator.body);
		assertEquals("html", mbReplyCreator.format);
		assertSame(ProgressCallback.NOOP, mbReplyCreator.progressCallback);
	}

	@Test
	void requestRejectsInvalidCount() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new MBReplyCreateRequest(71L, 1601L, 0, "", ""));
	}

	@Test
	void executeNormalizesPartialResults() throws Throwable {
		MBReplyCreateWorkflowAdapter adapter =
			new MBReplyCreateWorkflowAdapter(
				new StubMBReplyCreator(
					List.of(
						_mbReply(
							1301L, 1401L, 1501L, 1601L, "Reply 1", "Body 1"))));

		WorkflowStepResult<MBReplyStepItem> result = adapter.execute(
			new MBReplyCreateRequest(71L, 1601L, 2, "reply", "markdown"));

		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertEquals(
			"Only 1 of 2 MB replies were created.", result.error());
	}

	private static MBMessage _mbReply(
		long messageId, long groupId, long categoryId, long threadId,
		String subject, String body) {

		return TestModelProxyUtil.proxy(
			MBMessage.class,
			Map.of(
				"getBody", body,
				"getCategoryId", categoryId,
				"getGroupId", groupId,
				"getMessageId", messageId,
				"getSubject", subject,
				"getThreadId", threadId));
	}

	private static class StubMBReplyCreator extends MBReplyCreator {

		@Override
		public List<MBMessage> create(
			long userId, long threadId, int count, String body, String format,
			ProgressCallback progress) {

			this.body = body;
			this.count = count;
			this.format = format;
			this.progressCallback = progress;
			this.threadId = threadId;
			this.userId = userId;

			return _replies;
		}

		private StubMBReplyCreator(List<MBMessage> replies) {
			_replies = replies;
		}

		private String body;
		private int count;
		private String format;
		private ProgressCallback progressCallback;
		private final List<MBMessage> _replies;
		private long threadId;
		private long userId;

	}

}
