package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowDefinitionTest {

	@Test
	void constructorRejectsNullInput() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowDefinition("1.0", "workflow-1", null, List.of()));

		assertEquals("input is required", exception.getMessage());
	}

	@Test
	void constructorRejectsNullSteps() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowDefinition(
				"1.0", "workflow-1", Map.of(), null));

		assertEquals("steps is required", exception.getMessage());
	}

}
