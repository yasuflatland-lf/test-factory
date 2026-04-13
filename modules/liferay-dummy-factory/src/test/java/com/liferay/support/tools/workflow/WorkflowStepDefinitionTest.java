package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorkflowStepDefinitionTest {

	@Test
	void constructorRejectsNullParameters() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowStepDefinition(
				"step-1", "sample.operation", "idem-1", null,
				WorkflowErrorPolicy.FAIL_FAST));

		assertEquals("parameters is required", exception.getMessage());
	}

}
