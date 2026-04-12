package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.UserCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.core.dto.UserCreateRequest;

import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class UserCreateWorkflowAdapter
	implements WorkflowOperationAdapter<UserCreateRequest> {

	@Override
	public WorkflowStepResult execute(
			UserCreateRequest request, WorkflowAdapterContext context,
			ProgressCallback progress)
		throws Throwable {

		Objects.requireNonNull(request, "request is required");
		Objects.requireNonNull(context, "context is required");

		return WorkflowResultNormalizer.normalize(
			_userCreator.create(
				context.userId(), context.companyId(), request.toUserBatchSpec(),
				(progress == null) ? ProgressCallback.NOOP : progress));
	}

	@Override
	public String getOperationName() {
		return "user.create";
	}

	@Override
	public Class<UserCreateRequest> getRequestType() {
		return UserCreateRequest.class;
	}

	@Reference
	private UserCreator _userCreator;

}
