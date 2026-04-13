package com.liferay.support.tools.workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WorkflowPlan(
	WorkflowDefinition definition,
	Map<String, WorkflowStepDefinition> stepsById) {

	public WorkflowPlan {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}

		stepsById = Map.copyOf(new LinkedHashMap<>(stepsById));
	}

	public WorkflowStepDefinition getRequiredStep(String stepId) {
		WorkflowStepDefinition stepDefinition = stepsById.get(stepId);

		if (stepDefinition == null) {
			throw new IllegalArgumentException("unknown stepId: " + stepId);
		}

		return stepDefinition;
	}

	public List<WorkflowStepDefinition> orderedSteps() {
		return definition.steps();
	}

}
