package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.portal.kernel.model.Company;
import com.liferay.support.tools.service.CompanyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.adapter.core.dto.CompanyCreateRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = WorkflowOperationAdapter.class)
public class CompanyCreateWorkflowAdapter
	implements WorkflowOperationAdapter<CompanyCreateRequest> {

	@Override
	public WorkflowStepResult execute(
			CompanyCreateRequest request, WorkflowAdapterContext context,
			ProgressCallback progress)
		throws Throwable {

		Objects.requireNonNull(request, "request is required");

		List<Company> companies = _companyCreator.create(
			request.count(), request.webId(), request.virtualHostname(),
			request.mx(), request.maxUsers(), request.active(),
			(progress == null) ? ProgressCallback.NOOP : progress);

		List<Map<String, Object>> items = new ArrayList<>(companies.size());

		for (Company company : companies) {
			Map<String, Object> item = new LinkedHashMap<>();

			item.put("companyId", company.getCompanyId());
			item.put("webId", company.getWebId());

			items.add(item);
		}

		int createdCount = items.size();
		boolean success = (createdCount == request.count());
		String error = success ? null :
			"Only " + createdCount + " of " + request.count() +
				" companies were created.";

		return new WorkflowStepResult(
			request.count(), createdCount, 0, success, error, items);
	}

	@Override
	public String getOperationName() {
		return "company.create";
	}

	@Override
	public Class<CompanyCreateRequest> getRequestType() {
		return CompanyCreateRequest.class;
	}

	@Reference
	private CompanyCreator _companyCreator;

}
