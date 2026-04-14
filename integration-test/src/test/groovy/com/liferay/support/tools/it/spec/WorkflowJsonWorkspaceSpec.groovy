package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Response

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class WorkflowJsonWorkspaceSpec extends BaseLiferaySpec {

	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	private static final String WORKFLOW_JSON_SELECTOR_TEST_ID =
		'entity-selector-WORKFLOW_JSON'
	private static final String WORKFLOW_JSON_EDITOR_TEST_ID =
		'workflow-json-textarea'
	private static final String WORKFLOW_JSON_SAMPLE_LOAD_TEST_ID =
		'workflow-json-sample-load'
	private static final String WORKFLOW_JSON_VALIDATE_TEST_ID =
		'workflow-json-validate'
	private static final String WORKFLOW_JSON_PLAN_TEST_ID =
		'workflow-json-plan'
	private static final String WORKFLOW_JSON_EXECUTE_TEST_ID =
		'workflow-json-execute'
	private static final String USERS_SELECTOR_TEST_ID = 'entity-selector-USERS'
	private static final String USERS_COUNT_INPUT_TEST_ID = 'users-count-input'
	private static final String USERS_BASE_NAME_INPUT_TEST_ID =
		'users-base-name-input'
	private static final String USERS_SUBMIT_TEST_ID = 'users-submit'
	private static final String USERS_RESULT_TEST_ID = 'users-result'

	@Shared
	PlaywrightLifecycle pw

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		pw?.close()
	}

	def 'Workflow JSON workspace opens, loads a sample, validates it, plans it, and executes it'() {
		given:
		Page page = pw.page

		when: 'the portlet is opened'
		_openPortlet(page)

		and: 'the Workflow JSON workspace is selected'
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_SELECTOR_TEST_ID}\"]"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_SELECTOR_TEST_ID}\"]"
		).click()

		and: 'the JSON editor is available'
		Locator editor = page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EDITOR_TEST_ID}\"]"
		)
		editor.waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		and: 'a sample is loaded into the editor'
		String initialEditorValue = editor.inputValue()

		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_SAMPLE_LOAD_TEST_ID}\"]"
		).first().waitFor(new Locator.WaitForOptions().setTimeout(30_000))
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_SAMPLE_LOAD_TEST_ID}\"]"
		).first().click()

		String editorJson = _waitForEditorJson(editor, initialEditorValue)
		Map<String, Object> loadedWorkflowRequest = _normalizeWorkflowRequest(
			new JsonSlurper().parseText(editorJson) as Map<String, Object>)

		editor.fill(JsonOutput.prettyPrint(JsonOutput.toJson(loadedWorkflowRequest)))
		String normalizedEditorJson = editor.inputValue()

		then: 'the loaded sample is valid workflow JSON'
		(normalizedEditorJson as String).contains('"schemaVersion"')
		(loadedWorkflowRequest.schemaVersion as String) == '1.0'
		((loadedWorkflowRequest.steps ?: []) as List).size() > 0

		when: 'validation is triggered from the workspace'
		Response validateResponse = page.waitForResponse(
			{ Response response ->
				response.request().method() == 'POST' &&
					response.url().contains('/o/ldf-workflow/plan')
			},
			{
				page.locator(
					"[data-testid=\"${WORKFLOW_JSON_VALIDATE_TEST_ID}\"]"
				).click()
			}
		)

		Map<String, Object> validationResponse = new JsonSlurper().parseText(
			validateResponse.text()) as Map<String, Object>

		then: 'the server accepts the sample via the plan endpoint'
		validateResponse.status() == 200
		(validationResponse.errors ?: []) == []
		(validationResponse.plan as Map<String, Object>) != null
		(((validationResponse.plan as Map<String, Object>).definition as Map<String, Object>).steps as List).size() > 0

		when: 'planning is triggered from the workspace'
		Response planResponse = page.waitForResponse(
			{ Response response ->
				response.request().method() == 'POST' &&
					response.url().contains('/o/ldf-workflow/plan')
			},
			{
				page.locator(
					"[data-testid=\"${WORKFLOW_JSON_PLAN_TEST_ID}\"]"
				).click()
			}
		)

		Map<String, Object> planResponseBody = new JsonSlurper().parseText(
			planResponse.text()) as Map<String, Object>

		then: 'the plan endpoint returns a rendered workflow plan'
		planResponse.status() == 200
		(planResponseBody.errors ?: []) == []
		(planResponseBody.plan as Map<String, Object>) != null
		(((planResponseBody.plan as Map<String, Object>).definition as Map<String, Object>).steps as List).size() > 0

		when: 'execution is triggered from the workspace'
		Response executeResponse = page.waitForResponse(
			{ Response response ->
				response.request().method() == 'POST' &&
					response.url().contains('/o/ldf-workflow/execute')
			},
			{
				page.locator(
					"[data-testid=\"${WORKFLOW_JSON_EXECUTE_TEST_ID}\"]"
				).click()
			}
		)

		Map<String, Object> executionResponse = new JsonSlurper().parseText(
			executeResponse.text()) as Map<String, Object>

		then: 'the loaded sample executes successfully'
		executeResponse.status() == 200
		((executionResponse.execution as Map<String, Object>).status as String) ==
			'SUCCEEDED'
		(((executionResponse.execution as Map<String, Object>).steps ?: []) as List).size() > 0
		(((executionResponse.execution as Map<String, Object>).steps ?: []) as List<Map<String, Object>>).every {
			(it.status as String) == 'SUCCEEDED'
		}
	}

	def 'Legacy entity form still renders and submits after switching away from Workflow JSON'() {
		given:
		Page page = pw.page

		when: 'the portlet is opened'
		_openPortlet(page)

		and: 'the Workflow JSON selector is visited first'
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_SELECTOR_TEST_ID}\"]"
		).click()
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EDITOR_TEST_ID}\"]"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		and: 'the legacy Users form is selected again'
		page.locator(
			"[data-testid=\"${USERS_SELECTOR_TEST_ID}\"]"
		).click()
		page.locator(
			"[data-testid=\"${USERS_COUNT_INPUT_TEST_ID}\"]"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		and: 'the users form is filled and submitted'
		page.locator("[data-testid=\"${USERS_COUNT_INPUT_TEST_ID}\"]").fill('1')
		page.locator("[data-testid=\"${USERS_BASE_NAME_INPUT_TEST_ID}\"]").fill(
			"WorkflowJsonLegacyUser${RUN_SUFFIX}"
		)

		Response response = page.waitForResponse(
			{ Response r -> r.url().contains('p_p_resource_id=%2Fldf%2Fuser') },
			{
				page.locator("[data-testid=\"${USERS_SUBMIT_TEST_ID}\"]").click()
			})

		then: 'the legacy form still submits successfully'
		response.status() == 200
		page.locator(
			"[data-testid=\"${USERS_RESULT_TEST_ID}\"].alert-success"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))
		page.locator(
			"[data-testid=\"${USERS_RESULT_TEST_ID}\"].alert-success"
		).isVisible()
	}

	private static void _openPortlet(Page page) {
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()
	}

	private static Map<String, Object> _normalizeWorkflowRequest(
		Map<String, Object> workflowRequest) {

		Map<String, Object> normalizedRequest = new LinkedHashMap<>(
			workflowRequest ?: [:]
		)

		normalizedRequest.workflowId =
			"workflow-json-workspace-it-${RUN_SUFFIX}"

		List<Map<String, Object>> normalizedSteps = []

		((workflowRequest.steps ?: []) as List<Map<String, Object>>).eachWithIndex {
			Map<String, Object> step, int index ->

			Map<String, Object> normalizedStep = new LinkedHashMap<>(step)

			normalizedStep.idempotencyKey =
				"${step.idempotencyKey ?: "workflow-json-step-${index}"}-${RUN_SUFFIX}"
			normalizedStep.params = _normalizeStepParams(
				(step.params ?: []) as List<Map<String, Object>>
			)

			normalizedSteps.add(normalizedStep)
		}

		normalizedRequest.steps = normalizedSteps

		return normalizedRequest
	}

	private static List<Map<String, Object>> _normalizeStepParams(
		List<Map<String, Object>> params) {

		return params.collect { Map<String, Object> param ->
			Map<String, Object> normalizedParam = new LinkedHashMap<>(param)
			String name = param.name as String

			if (name == 'count') {
				normalizedParam.value = 1
			}
			else if ([
				'baseName',
				'description',
				'mx',
				'name',
				'virtualHostname',
				'webId'
			].contains(name) && (param.value != null)) {
				normalizedParam.value = "${param.value}-${RUN_SUFFIX}"
			}

			return normalizedParam
		}
	}

	private static String _waitForEditorJson(
		Locator editor, String previousValue) {

		long deadline = System.currentTimeMillis() + 30_000L

		while (System.currentTimeMillis() < deadline) {
			String currentValue = editor.inputValue()

			if (currentValue?.trim() && (currentValue != previousValue)) {
				return currentValue
			}

			Thread.sleep(200)
		}

		throw new IllegalStateException(
			'Workflow JSON sample did not populate the editor before timeout')
	}

}
