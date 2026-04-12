package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.OrganizationCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.core.dto.OrganizationCreateRequest;

import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class OrganizationCreateWorkflowAdapter
	implements WorkflowOperationAdapter<OrganizationCreateRequest> {

	@Override
	public WorkflowStepResult execute(
			OrganizationCreateRequest request, WorkflowAdapterContext context,
			ProgressCallback progress)
		throws Throwable {

		Objects.requireNonNull(request, "request is required");
		Objects.requireNonNull(context, "context is required");

		return WorkflowResultNormalizer.normalize(
			_organizationCreator.create(
				context.userId(), request.batch(),
				request.parentOrganizationId(), request.site(),
				(progress == null) ? ProgressCallback.NOOP : progress));
	}

	@Override
	public String getOperationName() {
		return "organization.create";
	}

	@Override
	public Class<OrganizationCreateRequest> getRequestType() {
		return OrganizationCreateRequest.class;
	}

	@Reference
	private OrganizationCreator _organizationCreator;

}
