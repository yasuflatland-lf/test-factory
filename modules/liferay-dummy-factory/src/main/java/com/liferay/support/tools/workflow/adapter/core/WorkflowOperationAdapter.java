package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.utils.ProgressCallback;

public interface WorkflowOperationAdapter<T> {

	WorkflowStepResult execute(
			T request, WorkflowAdapterContext context, ProgressCallback progress)
		throws Throwable;

	String getOperationName();

	Class<T> getRequestType();

	default WorkflowStepResult execute(
			T request, WorkflowAdapterContext context)
		throws Throwable {

		return execute(request, context, ProgressCallback.NOOP);
	}

}
