package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowFunctionFactoryTest {

	@Test
	void createSupportsUnknownSpiOperationWithGenericDescriptor()
		throws Exception {

		WorkflowOperationAdapter adapter = new WorkflowOperationAdapter() {

			@Override
			public com.liferay.support.tools.workflow.spi.WorkflowStepResult execute(
				WorkflowExecutionContext workflowExecutionContext,
				Map<String, Object> parameters) {

				return new com.liferay.support.tools.workflow.spi.WorkflowStepResult(
					true, 1, 1, 0, List.of(Map.of("ok", true)), null);
			}

			@Override
			public String operationName() {
				return "custom.dynamic.operation";
			}
		};

		WorkflowFunction workflowFunction = new WorkflowFunctionFactory().create(
			adapter);

		assertEquals("custom.dynamic.operation", workflowFunction.operation());
		assertNotNull(workflowFunction.executor());
		assertNotNull(
			((DefaultWorkflowFunction)workflowFunction).descriptor());

		WorkflowStepResult result = workflowFunction.executor().execute(
			new WorkflowStepExecutionRequest(
				"w-1", "s-1", "custom.dynamic.operation", "idem-1", Map.of(),
				new DefaultWorkflowExecutionContext(Map.of("userId", 1001L))));

		assertEquals(true, result.success());
		assertEquals(1, result.count());
	}

}
