package com.liferay.support.tools.workflow.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowStepResultTest {

	@Test
	void fromItemsNormalizesFullSuccess() {
		WorkflowStepResult<String> result = WorkflowStepResult.fromItems(
			"category.create", 2, List.of("a", "b"), value -> value,
			"categories");

		assertEquals("category.create", result.operation());
		assertEquals(2, result.requested());
		assertEquals(2, result.count());
		assertEquals(0, result.skipped());
		assertTrue(result.success());
		assertEquals(List.of("a", "b"), result.items());
	}

	@Test
	void fromItemsNormalizesPartialResult() {
		WorkflowStepResult<String> result = WorkflowStepResult.fromItems(
			"mbReply.create", 3, List.of("reply-1"), value -> value,
			"MB replies");

		assertEquals(1, result.count());
		assertEquals(2, result.skipped());
		assertFalse(result.success());
		assertEquals(
			"Only 1 of 3 MB replies were created.", result.error());
	}

	@Test
	void constructorRejectsNegativeRequested() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepResult<>(
				"vocabulary.create", -1, 0, 0, false, List.of(), null));
	}

}
