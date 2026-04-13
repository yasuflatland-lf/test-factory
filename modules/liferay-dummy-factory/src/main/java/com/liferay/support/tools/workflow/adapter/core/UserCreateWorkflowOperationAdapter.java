package com.liferay.support.tools.workflow.adapter.core;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.UserBatchSpec;
import com.liferay.support.tools.service.UserCreator;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.workflow.WorkflowParameterValues;
import com.liferay.support.tools.workflow.adapter.core.dto.UserCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;
import com.liferay.support.tools.workflow.spi.WorkflowStepResult;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter.class)
public class UserCreateWorkflowOperationAdapter
	implements com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter {

	@Override
	public WorkflowStepResult execute(
			WorkflowExecutionContext workflowExecutionContext,
			Map<String, Object> parameters)
		throws Throwable {

		WorkflowParameterValues values = new WorkflowParameterValues(parameters);
		UserCreateRequest request = new UserCreateRequest(
			new BatchSpec(values.requireCount(), values.requireText("baseName")),
			values.optionalString("emailDomain", "liferay.com"),
			values.optionalBoolean("fakerEnable", false),
			values.optionalBoolean("generatePersonalSiteLayouts", false),
			values.optionalPositiveLongArray("groupIds"),
			values.optionalString("jobTitle", ""),
			values.optionalString("locale", "en_US"),
			values.optionalBoolean("male", true),
			values.optionalPositiveLongArray("orgRoleIds"),
			values.optionalPositiveLongArray("organizationIds"),
			values.optionalString("password", "test"),
			values.optionalLong("privateLayoutSetPrototypeId", 0L),
			values.optionalLong("publicLayoutSetPrototypeId", 0L),
			values.optionalPositiveLongArray("roleIds"),
			values.optionalPositiveLongArray("siteRoleIds"),
			values.optionalPositiveLongArray("userGroupIds"));

		UserBatchSpec userBatchSpec = request.toUserBatchSpec();

		return _toSpiResult(
			WorkflowResultNormalizer.normalize(
				_userCreator.create(
				workflowExecutionContext.userId(),
				workflowExecutionContext.companyId(), userBatchSpec,
				ProgressCallback.NOOP)));
	}

	@Override
	public String operationName() {
		return "user.create";
	}

	@Reference
	private UserCreator _userCreator;

	private static WorkflowStepResult _toSpiResult(
		com.liferay.support.tools.workflow.adapter.core.WorkflowStepResult result) {

		return new WorkflowStepResult(
			result.success(), result.requested(), result.count(),
			result.skipped(), result.items(), result.error());
	}

}
