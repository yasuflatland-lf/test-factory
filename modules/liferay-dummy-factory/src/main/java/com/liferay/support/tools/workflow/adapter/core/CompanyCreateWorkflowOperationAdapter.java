package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.portal.kernel.model.Company;
import com.liferay.support.tools.service.CompanyCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class CompanyCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);

		List<Company> companies = _companyCreator.create(
			values.requireCount(), values.requireText("webId"),
			values.requireText("virtualHostname"), values.requireText("mx"),
			values.optionalInt("maxUsers", 0),
			values.optionalBoolean("active", true), ProgressCallback.NOOP);

		List<Map<String, Object>> items = new ArrayList<>(companies.size());

		for (Company company : companies) {
			Map<String, Object> item = new LinkedHashMap<>();

			item.put("companyId", company.getCompanyId());
			item.put("webId", company.getWebId());

			items.add(item);
		}

		int requested = values.requireCount();
		int createdCount = items.size();
		int skipped = requested - createdCount;
		boolean success = (createdCount == requested);
		String error = success ? null :
			"Only " + createdCount + " of " + requested + " companies were created.";

		return new WorkflowStepResult(
			success, requested, createdCount, skipped, items, error);
	}

	@Override
	public String operationName() {
		return "company.create";
	}

	@Reference
	private CompanyCreator _companyCreator;

}
