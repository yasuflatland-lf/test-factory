package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.SiteCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.core.dto.SiteCreateRequest;

import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class SiteCreateWorkflowAdapter
	implements WorkflowOperationAdapter<SiteCreateRequest> {

	@Override
	public WorkflowStepResult execute(
			SiteCreateRequest request, WorkflowAdapterContext context,
			ProgressCallback progress)
		throws Throwable {

		Objects.requireNonNull(request, "request is required");
		Objects.requireNonNull(context, "context is required");

		return WorkflowResultNormalizer.normalize(
			_siteCreator.create(
				context.userId(), context.companyId(), request.batch(),
				request.membershipType(), request.parentGroupId(),
				request.siteTemplateId(), request.manualMembership(),
				request.inheritContent(), request.active(),
				request.description(), request.publicLayoutSetPrototypeId(),
				request.privateLayoutSetPrototypeId(),
				(progress == null) ? ProgressCallback.NOOP : progress));
	}

	@Override
	public String getOperationName() {
		return "site.create";
	}

	@Override
	public Class<SiteCreateRequest> getRequestType() {
		return SiteCreateRequest.class;
	}

	@Reference
	private SiteCreator _siteCreator;

}
