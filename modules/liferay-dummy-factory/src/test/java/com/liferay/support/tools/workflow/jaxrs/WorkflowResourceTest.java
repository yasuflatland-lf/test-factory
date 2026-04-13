package com.liferay.support.tools.workflow.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.support.tools.workflow.dto.WorkflowParameterDto;
import com.liferay.support.tools.workflow.dto.WorkflowRequestDto;
import com.liferay.support.tools.workflow.dto.WorkflowStepDto;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowResourceTest {

	@Test
	void planRejectsUnsupportedSchemaVersion() {
		WorkflowResource workflowResource = new WorkflowResource();

		WorkflowRequestDto request = new WorkflowRequestDto(
			"2.0", "wf-1", Map.of(),
			List.of(
				new WorkflowStepDto(
					"step-1", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", 1, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		List<String> errorCodes = workflowResource.plan(
			request
		).errors(
		).stream(
		).map(
			error -> error.code()
		).toList();

		assertTrue(errorCodes.contains("SCHEMA_VERSION_UNSUPPORTED"));
	}

	@Test
	void planAcceptsSchemaVersion10() {
		WorkflowResource workflowResource = new WorkflowResource();

		WorkflowRequestDto request = new WorkflowRequestDto(
			"1.0", "wf-1", Map.of(),
			List.of(
				new WorkflowStepDto(
					"step-1", "unknown.operation", "idem-1",
					List.of(
						new WorkflowParameterDto("count", 1, null),
						new WorkflowParameterDto("baseName", "Demo", null)),
					null)));

		List<String> errorCodes = workflowResource.plan(
			request
		).errors(
		).stream(
		).map(
			error -> error.code()
		).toList();

		assertEquals(false, errorCodes.contains("SCHEMA_VERSION_UNSUPPORTED"));
	}

}
