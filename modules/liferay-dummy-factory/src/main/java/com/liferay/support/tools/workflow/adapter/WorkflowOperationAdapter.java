package com.liferay.support.tools.workflow.adapter;

public interface WorkflowOperationAdapter<T, R> {

	public WorkflowStepResult<R> execute(T request) throws Throwable;

	public String operation();

}
