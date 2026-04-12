package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.RoleCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.core.dto.RoleCreateRequest;

import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class RoleCreateWorkflowAdapter
	implements WorkflowOperationAdapter<RoleCreateRequest> {

	@Override
	public WorkflowStepResult execute(
			RoleCreateRequest request, WorkflowAdapterContext context,
			ProgressCallback progress)
		throws Throwable {

		Objects.requireNonNull(request, "request is required");
		Objects.requireNonNull(context, "context is required");

		return WorkflowResultNormalizer.normalize(
			_roleCreator.create(
				context.userId(), request.batch(), request.roleType(),
				request.description(),
				(progress == null) ? ProgressCallback.NOOP : progress));
	}

	@Override
	public String getOperationName() {
		return "role.create";
	}

	@Override
	public Class<RoleCreateRequest> getRequestType() {
		return RoleCreateRequest.class;
	}

	@Reference
	private RoleCreator _roleCreator;

}
