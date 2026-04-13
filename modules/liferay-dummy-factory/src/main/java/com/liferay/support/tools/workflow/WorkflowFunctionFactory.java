package com.liferay.support.tools.workflow;

import com.liferay.support.tools.service.BatchSpec;
import com.liferay.support.tools.service.RoleType;
import com.liferay.support.tools.service.SiteMembershipType;
import com.liferay.support.tools.workflow.adapter.core.WorkflowAdapterContext;
import com.liferay.support.tools.workflow.adapter.core.dto.CompanyCreateRequest;
import com.liferay.support.tools.workflow.adapter.core.dto.OrganizationCreateRequest;
import com.liferay.support.tools.workflow.adapter.core.dto.RoleCreateRequest;
import com.liferay.support.tools.workflow.adapter.core.dto.SiteCreateRequest;
import com.liferay.support.tools.workflow.adapter.core.dto.UserCreateRequest;
import com.liferay.support.tools.workflow.adapter.messageboards.MBCategoryCreateRequest;
import com.liferay.support.tools.workflow.adapter.messageboards.MBReplyCreateRequest;
import com.liferay.support.tools.workflow.adapter.messageboards.MBThreadCreateRequest;
import com.liferay.support.tools.workflow.adapter.taxonomy.CategoryCreateRequest;
import com.liferay.support.tools.workflow.adapter.taxonomy.VocabularyCreateRequest;
import com.liferay.support.tools.workflow.spi.WorkflowExecutionContext;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkflowFunctionFactory {

	public WorkflowFunction create(
		com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter<?, ?>
			adapter) {

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.taxonomy.
					VocabularyCreateWorkflowAdapter vocabularyCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(vocabularyCreateWorkflowAdapter.operation()),
				request -> _invoke(
					() -> _execute(
						request, vocabularyCreateWorkflowAdapter, values -> {
							BatchSpec batchSpec = _batchSpec(values);

							return new VocabularyCreateRequest(
								_userId(request, values),
								values.requirePositiveLong("groupId"), batchSpec);
						})));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.taxonomy.
					CategoryCreateWorkflowAdapter categoryCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(categoryCreateWorkflowAdapter.operation()),
				request -> _invoke(
					() -> _execute(
						request, categoryCreateWorkflowAdapter, values -> {
							BatchSpec batchSpec = _batchSpec(values);

							return new CategoryCreateRequest(
								_userId(request, values),
								values.requirePositiveLong("groupId"),
								values.requirePositiveLong("vocabularyId"), batchSpec);
						})));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.messageboards.
					MBCategoryCreateWorkflowAdapter
						mbCategoryCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(mbCategoryCreateWorkflowAdapter.operation()),
				request -> _invoke(
					() -> _execute(
						request, mbCategoryCreateWorkflowAdapter, values ->
							new MBCategoryCreateRequest(
								_userId(request, values),
								values.requirePositiveLong("groupId"),
								_batchSpec(values),
								values.requireText("description")))));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.messageboards.
					MBThreadCreateWorkflowAdapter mbThreadCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(mbThreadCreateWorkflowAdapter.operation()),
				request -> _invoke(
					() -> _execute(
						request, mbThreadCreateWorkflowAdapter, values ->
							new MBThreadCreateRequest(
								_userId(request, values),
								values.requirePositiveLong("groupId"),
								values.optionalLong("categoryId", 0L),
								_batchSpec(values), values.requireText("body"),
								values.optionalString("format", "html")))));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.messageboards.
					MBReplyCreateWorkflowAdapter mbReplyCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(mbReplyCreateWorkflowAdapter.operation()),
				request -> _invoke(
					() -> _execute(
						request, mbReplyCreateWorkflowAdapter, values ->
							new MBReplyCreateRequest(
								_userId(request, values),
								values.requirePositiveLong("threadId"),
								values.requireCount(), values.requireText("body"),
								values.optionalString("format", "html")))));
		}

		throw new IllegalArgumentException(
			"Unsupported workflow adapter: " + adapter.getClass().getName());
	}

	public WorkflowFunction create(
		com.liferay.support.tools.workflow.adapter.core.WorkflowOperationAdapter<?>
			adapter) {

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.core.
					CompanyCreateWorkflowAdapter companyCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(companyCreateWorkflowAdapter.getOperationName()),
				request -> _invoke(
					() -> _toStepResult(
						companyCreateWorkflowAdapter.execute(
							new CompanyCreateRequest(
								_values(request).optionalBoolean("active", true),
								_values(request).requireCount(),
								_values(request).optionalInt("maxUsers", 0),
								_values(request).requireText("mx"),
								_values(request).requireText("virtualHostname"),
								_values(request).requireText("webId")),
							new WorkflowAdapterContext(
								_userId(request, _values(request), false),
								_companyId(request, _values(request), false))))));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.core.
					SiteCreateWorkflowAdapter siteCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(siteCreateWorkflowAdapter.getOperationName()),
				request -> _invoke(
					() -> {
						WorkflowParameterValues values = _values(request);

						return _toStepResult(
							siteCreateWorkflowAdapter.execute(
								new SiteCreateRequest(
									values.optionalBoolean("active", true),
									_batchSpec(values),
									values.optionalString("description", ""),
									values.optionalBoolean("inheritContent", false),
									values.optionalBoolean("manualMembership", true),
									values.optionalEnum(
										"membershipType", SiteMembershipType::fromString,
										SiteMembershipType.OPEN),
									values.optionalLong("parentGroupId", 0L),
									values.optionalLong("privateLayoutSetPrototypeId", 0L),
									values.optionalLong("publicLayoutSetPrototypeId", 0L),
									values.optionalLong("siteTemplateId", 0L)),
								new WorkflowAdapterContext(
									_userId(request, values),
									_companyId(request, values))));
					}));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.core.
					OrganizationCreateWorkflowAdapter
						organizationCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(organizationCreateWorkflowAdapter.getOperationName()),
				request -> _invoke(
					() -> {
						WorkflowParameterValues values = _values(request);

						return _toStepResult(
							organizationCreateWorkflowAdapter.execute(
								new OrganizationCreateRequest(
									_batchSpec(values),
									values.optionalLong("parentOrganizationId", 0L),
									values.optionalBoolean("site", false)),
								new WorkflowAdapterContext(
									_userId(request, values),
									_companyId(request, values))));
					}));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.core.
					RoleCreateWorkflowAdapter roleCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(roleCreateWorkflowAdapter.getOperationName()),
				request -> _invoke(
					() -> {
						WorkflowParameterValues values = _values(request);

						return _toStepResult(
							roleCreateWorkflowAdapter.execute(
								new RoleCreateRequest(
									_batchSpec(values),
									values.optionalString("description", ""),
									values.optionalEnum(
										"roleType", RoleType::fromString,
										RoleType.REGULAR)),
								new WorkflowAdapterContext(
									_userId(request, values),
									_companyId(request, values))));
					}));
		}

		if (adapter instanceof
				com.liferay.support.tools.workflow.adapter.core.
					UserCreateWorkflowAdapter userCreateWorkflowAdapter) {

			return new DefaultWorkflowFunction(
				_descriptor(userCreateWorkflowAdapter.getOperationName()),
				request -> _invoke(
					() -> {
						WorkflowParameterValues values = _values(request);

						return _toStepResult(
							userCreateWorkflowAdapter.execute(
								new UserCreateRequest(
									_batchSpec(values),
									values.optionalString("emailDomain", "liferay.com"),
									values.optionalBoolean("fakerEnable", false),
									values.optionalBoolean(
										"generatePersonalSiteLayouts", false),
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
									values.optionalPositiveLongArray("userGroupIds")),
								new WorkflowAdapterContext(
									_userId(request, values),
									_companyId(request, values))));
					}));
		}

		throw new IllegalArgumentException(
			"Unsupported workflow core adapter: " + adapter.getClass().getName());
	}

	public WorkflowFunction create(
		com.liferay.support.tools.workflow.spi.WorkflowOperationAdapter adapter) {

		return new DefaultWorkflowFunction(
			_descriptor(adapter.operationName()),
			request -> _invoke(
				() -> _toStepResult(
					adapter.execute(
						new WorkflowExecutionContext(_userId(request, _values(request))),
						request.parameters()))));
	}

	public WorkflowFunctionDescriptor descriptor(String operation) {
		return _descriptor(operation);
	}

	private static BatchSpec _batchSpec(WorkflowParameterValues values) {
		return new BatchSpec(values.requireCount(), values.requireText("baseName"));
	}

	private static long _companyId(
		WorkflowStepExecutionRequest request, WorkflowParameterValues values) {

		return _companyId(request, values, true);
	}

	private static long _companyId(
		WorkflowStepExecutionRequest request, WorkflowParameterValues values,
		boolean required) {

		long companyId = values.optionalLong(
			"companyId", _runtimeCompanyId(request.context()));

		if (required && (companyId <= 0)) {
			throw new IllegalArgumentException("companyId is required");
		}

		return companyId;
	}

	private static WorkflowFunctionDescriptor _descriptor(String operation) {
		WorkflowFunctionDescriptor descriptor = _DESCRIPTORS.get(operation);

		if (descriptor == null) {
			throw new IllegalArgumentException(
				"Unknown workflow descriptor: " + operation);
		}

		return descriptor;
	}

	private static <T> WorkflowStepResult _execute(
			WorkflowStepExecutionRequest request,
			com.liferay.support.tools.workflow.adapter.WorkflowOperationAdapter
				<T, ?> adapter,
			RequestFactory<T> requestFactory)
		throws Exception {

		try {
			return _toStepResult(adapter.execute(requestFactory.create(_values(request))));
		}
		catch (Exception exception) {
			throw exception;
		}
		catch (Throwable throwable) {
			throw new Exception(throwable);
		}
	}

	private static WorkflowStepResult _invoke(
			ThrowingWorkflowStepResultSupplier throwingWorkflowStepResultSupplier)
		throws Exception {

		try {
			return throwingWorkflowStepResultSupplier.get();
		}
		catch (Exception exception) {
			throw exception;
		}
		catch (Throwable throwable) {
			throw new Exception(throwable);
		}
	}

	private static long _inputLong(
		WorkflowExecutionContextView workflowExecutionContextView, String key) {

		Object value = workflowExecutionContextView.input().get(key);

		if (value instanceof Number number) {
			return number.longValue();
		}

		if (value instanceof String string) {
			try {
				return Long.parseLong(string.trim());
			}
			catch (NumberFormatException numberFormatException) {
				return 0L;
			}
		}

		return 0L;
	}

	private static long _runtimeCompanyId(
		WorkflowExecutionContextView workflowExecutionContextView) {

		long inputCompanyId = _inputLong(workflowExecutionContextView, "companyId");

		if (inputCompanyId > 0) {
			return inputCompanyId;
		}

		if (workflowExecutionContextView instanceof DefaultWorkflowExecutionContext
				defaultWorkflowExecutionContext) {

			return defaultWorkflowExecutionContext.currentCompanyId();
		}

		return 0L;
	}

	private static long _runtimeUserId(
		WorkflowExecutionContextView workflowExecutionContextView) {

		long inputUserId = _inputLong(workflowExecutionContextView, "userId");

		if (inputUserId > 0) {
			return inputUserId;
		}

		if (workflowExecutionContextView instanceof DefaultWorkflowExecutionContext
				defaultWorkflowExecutionContext) {

			return defaultWorkflowExecutionContext.currentUserId();
		}

		return 0L;
	}

	private static WorkflowStepResult _toStepResult(
		com.liferay.support.tools.workflow.adapter.WorkflowStepResult<?> result) {

		List<Map<String, Object>> items = new ArrayList<>(result.items().size());

		for (Object item : result.items()) {
			items.add(_toMap(item));
		}

		return new WorkflowStepResult(
			result.success(), result.requested(), result.count(),
			result.skipped(), result.error(), items, Map.of());
	}

	private static WorkflowStepResult _toStepResult(
		com.liferay.support.tools.workflow.adapter.core.WorkflowStepResult result) {

		return new WorkflowStepResult(
			result.success(), result.requested(), result.count(),
			result.skipped(), result.error(), result.items(), Map.of());
	}

	private static WorkflowStepResult _toStepResult(
		com.liferay.support.tools.workflow.spi.WorkflowStepResult result) {

		return new WorkflowStepResult(
			result.success(), (int)result.requested(), (int)result.count(),
			(int)result.skipped(), result.error(), result.items(), Map.of());
	}

	private static Map<String, Object> _toMap(Object value) {
		if (value == null) {
			return Map.of();
		}

		if (value instanceof Map<?, ?> map) {
			Map<String, Object> normalized = new LinkedHashMap<>();

			for (Map.Entry<?, ?> entry : map.entrySet()) {
				normalized.put(String.valueOf(entry.getKey()), entry.getValue());
			}

			return Map.copyOf(normalized);
		}

		if (value.getClass().isRecord()) {
			return _toRecordMap(value);
		}

		return Map.of("value", value);
	}

	private static Map<String, Object> _toRecordMap(Object record) {
		Map<String, Object> values = new LinkedHashMap<>();

		for (RecordComponent recordComponent :
				record.getClass().getRecordComponents()) {

			try {
				values.put(
					recordComponent.getName(),
					recordComponent.getAccessor().invoke(record));
			}
			catch (ReflectiveOperationException reflectiveOperationException) {
				throw new IllegalStateException(
					"Unable to read record component: " +
						recordComponent.getName(),
					reflectiveOperationException);
			}
		}

		return Map.copyOf(values);
	}

	private static long _userId(
		WorkflowStepExecutionRequest request, WorkflowParameterValues values) {

		return _userId(request, values, true);
	}

	private static long _userId(
		WorkflowStepExecutionRequest request, WorkflowParameterValues values,
		boolean required) {

		long userId = values.optionalLong("userId", _runtimeUserId(request.context()));

		if (required && (userId <= 0)) {
			throw new IllegalArgumentException("userId is required");
		}

		return userId;
	}

	private static WorkflowParameterValues _values(
		WorkflowStepExecutionRequest request) {

		return new WorkflowParameterValues(request.parameters());
	}

	private static WorkflowFunctionParameter _parameter(
		String name, String type, boolean required, String description,
		Object defaultValue) {

		return new WorkflowFunctionParameter(
			name, type, required, description, defaultValue);
	}

	private interface RequestFactory<T> {

		public T create(WorkflowParameterValues values);

	}

	@FunctionalInterface
	private interface ThrowingWorkflowStepResultSupplier {

		public WorkflowStepResult get() throws Throwable;

	}

	private static final Map<String, WorkflowFunctionDescriptor> _DESCRIPTORS =
		Map.ofEntries(
			Map.entry(
				"blogs.create",
				new WorkflowFunctionDescriptor(
					"blogs.create", "Create blog entries in a target site.",
					List.of(
						_parameter("count", "integer", true, "Number of blogs to create.", null),
						_parameter("baseName", "string", true, "Base title used for generated blogs.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("userId", "long", false, "Override execution user id.", "current user"),
						_parameter("content", "string", false, "Body content.", ""),
						_parameter("subtitle", "string", false, "Blog subtitle.", ""),
						_parameter("description", "string", false, "Blog description.", ""),
						_parameter("allowPingbacks", "boolean", false, "Whether pingbacks are enabled.", false),
						_parameter("allowTrackbacks", "boolean", false, "Whether trackbacks are enabled.", false),
						_parameter("trackbackURLs", "string[]", false, "Trackback target URLs.", List.of())),
					"WorkflowStepResult")),
			Map.entry(
				"category.create",
				new WorkflowFunctionDescriptor(
					"category.create", "Create asset categories in a vocabulary.",
					List.of(
						_parameter("count", "integer", true, "Number of categories to create.", null),
						_parameter("baseName", "string", true, "Base category name.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("vocabularyId", "long", true, "Target vocabulary id.", null),
						_parameter("userId", "long", false, "Override execution user id.", "current user")),
					"WorkflowStepResult")),
			Map.entry(
				"company.create",
				new WorkflowFunctionDescriptor(
					"company.create", "Create virtual instances (companies).",
					List.of(
						_parameter("count", "integer", true, "Number of companies to create.", null),
						_parameter("webId", "string", true, "Company web id prefix.", null),
						_parameter("virtualHostname", "string", true, "Virtual host name.", null),
						_parameter("mx", "string", true, "Mail domain.", null),
						_parameter("maxUsers", "integer", false, "Maximum users for the company.", 0),
						_parameter("active", "boolean", false, "Whether the company is active.", true)),
					"WorkflowStepResult")),
			Map.entry(
				"document.create",
				new WorkflowFunctionDescriptor(
					"document.create", "Create documents in a target folder.",
					List.of(
						_parameter("count", "integer", true, "Number of documents to create.", null),
						_parameter("baseName", "string", true, "Base document title.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("folderId", "long", false, "Target folder id.", 0),
						_parameter("description", "string", false, "Document description.", ""),
						_parameter("uploadedFiles", "string[]", false, "Template file paths.", List.of())),
					"WorkflowStepResult")),
			Map.entry(
				"layout.create",
				new WorkflowFunctionDescriptor(
					"layout.create", "Create pages in a target site.",
					List.of(
						_parameter("count", "integer", true, "Number of pages to create.", null),
						_parameter("baseName", "string", true, "Base page name.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("type", "string", false, "Layout type.", "portlet"),
						_parameter("privateLayout", "boolean", false, "Whether pages are private.", false),
						_parameter("hidden", "boolean", false, "Whether pages are hidden.", false)),
					"WorkflowStepResult")),
			Map.entry(
				"mbCategory.create",
				new WorkflowFunctionDescriptor(
					"mbCategory.create", "Create message boards categories.",
					List.of(
						_parameter("count", "integer", true, "Number of categories to create.", null),
						_parameter("baseName", "string", true, "Base category name.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("description", "string", true, "Category description.", null),
						_parameter("userId", "long", false, "Override execution user id.", "current user")),
					"WorkflowStepResult")),
			Map.entry(
				"mbReply.create",
				new WorkflowFunctionDescriptor(
					"mbReply.create", "Create replies in a message board thread.",
					List.of(
						_parameter("count", "integer", true, "Number of replies to create.", null),
						_parameter("threadId", "long", true, "Target thread id.", null),
						_parameter("body", "string", true, "Reply body.", null),
						_parameter("format", "string", false, "Reply format.", "html"),
						_parameter("userId", "long", false, "Override execution user id.", "current user")),
					"WorkflowStepResult")),
			Map.entry(
				"mbThread.create",
				new WorkflowFunctionDescriptor(
					"mbThread.create", "Create message board threads.",
					List.of(
						_parameter("count", "integer", true, "Number of threads to create.", null),
						_parameter("baseName", "string", true, "Base thread subject.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("categoryId", "long", false, "Target category id. Use 0 for root.", 0),
						_parameter("body", "string", true, "Thread body.", null),
						_parameter("format", "string", false, "Thread body format.", "html"),
						_parameter("userId", "long", false, "Override execution user id.", "current user")),
					"WorkflowStepResult")),
			Map.entry(
				"organization.create",
				new WorkflowFunctionDescriptor(
					"organization.create", "Create organizations.",
					List.of(
						_parameter("count", "integer", true, "Number of organizations to create.", null),
						_parameter("baseName", "string", true, "Base organization name.", null),
						_parameter("parentOrganizationId", "long", false, "Parent organization id.", 0),
						_parameter("site", "boolean", false, "Whether to create an organization site.", false)),
					"WorkflowStepResult")),
			Map.entry(
				"role.create",
				new WorkflowFunctionDescriptor(
					"role.create", "Create roles.",
					List.of(
						_parameter("count", "integer", true, "Number of roles to create.", null),
						_parameter("baseName", "string", true, "Base role name.", null),
						_parameter("roleType", "string", false, "Role type.", "regular"),
						_parameter("description", "string", false, "Role description.", "")),
					"WorkflowStepResult")),
			Map.entry(
				"site.create",
				new WorkflowFunctionDescriptor(
					"site.create", "Create sites.",
					List.of(
						_parameter("count", "integer", true, "Number of sites to create.", null),
						_parameter("baseName", "string", true, "Base site name.", null),
						_parameter("membershipType", "string", false, "Membership type: open, restricted, private.", "open"),
						_parameter("parentGroupId", "long", false, "Parent site group id.", 0),
						_parameter("siteTemplateId", "long", false, "Site template id.", 0),
						_parameter("publicLayoutSetPrototypeId", "long", false, "Public layout set prototype id.", 0),
						_parameter("privateLayoutSetPrototypeId", "long", false, "Private layout set prototype id.", 0),
						_parameter("manualMembership", "boolean", false, "Whether manual membership is required.", true),
						_parameter("inheritContent", "boolean", false, "Whether content is inherited.", false),
						_parameter("active", "boolean", false, "Whether the site is active.", true),
						_parameter("description", "string", false, "Site description.", "")),
					"WorkflowStepResult")),
			Map.entry(
				"user.create",
				new WorkflowFunctionDescriptor(
					"user.create", "Create users.",
					List.of(
						_parameter("count", "integer", true, "Number of users to create.", null),
						_parameter("baseName", "string", true, "Base user name.", null),
						_parameter("emailDomain", "string", false, "Email domain.", "liferay.com"),
						_parameter("password", "string", false, "Default password.", "test"),
						_parameter("male", "boolean", false, "Whether generated users are male.", true),
						_parameter("jobTitle", "string", false, "Job title.", ""),
						_parameter("fakerEnable", "boolean", false, "Use faker profile generation.", false),
						_parameter("locale", "string", false, "Locale id.", "en_US"),
						_parameter("organizationIds", "long[]", false, "Organization ids.", List.of()),
						_parameter("roleIds", "long[]", false, "Role ids.", List.of()),
						_parameter("userGroupIds", "long[]", false, "User group ids.", List.of()),
						_parameter("siteRoleIds", "long[]", false, "Site role ids.", List.of()),
						_parameter("orgRoleIds", "long[]", false, "Organization role ids.", List.of()),
						_parameter("groupIds", "long[]", false, "Site group ids.", List.of()),
						_parameter("generatePersonalSiteLayouts", "boolean", false, "Generate personal site layouts.", false),
						_parameter("publicLayoutSetPrototypeId", "long", false, "Public layout set prototype id.", 0),
						_parameter("privateLayoutSetPrototypeId", "long", false, "Private layout set prototype id.", 0)),
					"WorkflowStepResult")),
			Map.entry(
				"vocabulary.create",
				new WorkflowFunctionDescriptor(
					"vocabulary.create", "Create asset vocabularies.",
					List.of(
						_parameter("count", "integer", true, "Number of vocabularies to create.", null),
						_parameter("baseName", "string", true, "Base vocabulary name.", null),
						_parameter("groupId", "long", true, "Target site group id.", null),
						_parameter("userId", "long", false, "Override execution user id.", "current user")),
					"WorkflowStepResult")),
			Map.entry(
				"webContent.create",
				new WorkflowFunctionDescriptor(
					"webContent.create", "Create web content articles.",
					List.of(
						_parameter("count", "integer", true, "Number of web contents to create.", null),
						_parameter("baseName", "string", true, "Base article title.", null),
						_parameter("groupIds", "long[]", true, "Target site group ids.", null),
						_parameter("folderId", "long", false, "Target folder id.", 0),
						_parameter("locales", "string[]", false, "Explicit locales. If omitted, site default locale is used.", List.of()),
						_parameter("neverExpire", "boolean", false, "Whether the content never expires.", true),
						_parameter("neverReview", "boolean", false, "Whether the content never requires review.", true),
						_parameter("createContentsType", "integer", false, "Content creation mode.", 0),
						_parameter("baseArticle", "string", false, "Base article content.", ""),
						_parameter("titleWords", "integer", false, "Generated title word count.", 5),
						_parameter("totalParagraphs", "integer", false, "Generated paragraph count.", 3),
						_parameter("randomAmount", "integer", false, "Random content amount.", 3),
						_parameter("linkLists", "string", false, "Link list configuration.", ""),
						_parameter("ddmStructureId", "long", false, "DDM structure id.", 0),
						_parameter("ddmTemplateId", "long", false, "DDM template id.", 0)),
					"WorkflowStepResult")));

}
