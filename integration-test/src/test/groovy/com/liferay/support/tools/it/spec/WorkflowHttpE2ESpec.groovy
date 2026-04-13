package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.liferay.support.tools.it.util.WorkflowHttpClient

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class WorkflowHttpE2ESpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		WorkflowHttpE2ESpec)

	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	private static final String USER_BASE_NAME = "WFHttpUser${RUN_SUFFIX}"
	private static final String SITE_BASE_NAME = "WFHttpSite${RUN_SUFFIX}"
	private static final String ARTICLE_BASE_NAME = "WFHttpArticle${RUN_SUFFIX}"

	@Shared
	PlaywrightLifecycle pw

	@Shared
	WorkflowHttpClient workflowHttpClient

	@Shared
	Long createdGroupId

	@Shared
	Long createdUserId

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		loginAsAdmin(pw)

		workflowHttpClient = new WorkflowHttpClient(liferay.baseUrl, pw.page)
	}

	def cleanupSpec() {
		if (createdGroupId) {
			try {
				jsonwsPost(
					'/api/jsonws/group/delete-group',
					['groupId': createdGroupId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up site {}: {}', createdGroupId, e.message)
			}
		}

		if (createdUserId) {
			try {
				jsonwsPost(
					'/api/jsonws/user/delete-user',
					['userId': createdUserId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up user {}: {}', createdUserId, e.message)
			}
		}

		workflowHttpClient = null
		pw?.close()
	}

	def 'plan exposes the three workflow steps in order'() {
		when:
		Map response = workflowHttpClient.plan(_workflowRequest())

		then:
		response.errors == []

		and:
		List<Map<String, Object>> steps = (
			(response.plan as Map).definition as Map
		).steps as List<Map<String, Object>>

		steps*.id == ['createUser', 'createSite', 'createWebContent']
		steps*.operation == [
			'user.create', 'site.create', 'webContent.create'
		]
	}

	def 'execute returns nested ids and counts through HTTP JSON'() {
		when:
		Map response = workflowHttpClient.execute(_workflowRequest())

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps*.stepId == ['createUser', 'createSite', 'createWebContent']
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map userStep = _stepById(steps, 'createUser')
		Map siteStep = _stepById(steps, 'createSite')
		Map webContentStep = _stepById(steps, 'createWebContent')

		Map userResult = userStep.result as Map
		Map siteResult = siteStep.result as Map
		Map webContentResult = webContentStep.result as Map

		Map userItem = (userResult.items as List<Map<String, Object>>).first()
		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		Map webContentItem =
			(webContentResult.items as List<Map<String, Object>>).first()

		createdUserId = userItem.userId as Long
		createdGroupId = siteItem.groupId as Long

		then:
		(userResult.count as Number) == 1
		(userItem.userId as Long) > 0

		and:
		(siteResult.count as Number) == 1
		(siteItem.groupId as Long) > 0

		and:
		(webContentResult.count as Number) == 1
		(webContentItem.created as Number) == 1
		(webContentItem.groupId as Long) == createdGroupId

		and:
		Map createdUser = jsonwsGet(
			"/api/jsonws/user/get-user-by-id/user-id/${createdUserId}") as Map
		createdUser.userId as Long == createdUserId
		(createdUser.screenName as String) == "${USER_BASE_NAME.toLowerCase()}1"

		and:
		Map createdSite = jsonwsGet(
			"/api/jsonws/group/get-group/group-id/${createdGroupId}") as Map
		createdSite.groupId as Long == createdGroupId

		and:
		int articleCount = jsonwsGet(
			"/api/jsonws/journal.journalarticle/get-articles-count" +
			"/group-id/${createdGroupId}/folder-id/0") as int
		articleCount == 1
	}

	private static Map<String, Object> _stepById(
		List<Map<String, Object>> steps, String stepId) {

		Map<String, Object> step = steps.find {
			(it.stepId as String) == stepId
		} as Map<String, Object>

		assert step != null

		return step
	}

	private Map<String, Object> _workflowRequest() {
		return [
			schemaVersion: '1.0',
			workflowId   : "workflow-http-e2e-${RUN_SUFFIX}",
			input        : [:],
			steps        : [
				[
					id             : 'createUser',
					idempotencyKey : "user-${RUN_SUFFIX}",
					operation      : 'user.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: USER_BASE_NAME]
					]
				],
				[
					id             : 'createSite',
					idempotencyKey : "site-${RUN_SUFFIX}",
					operation      : 'site.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: SITE_BASE_NAME]
					]
				],
				[
					id             : 'createWebContent',
					idempotencyKey : "web-content-${RUN_SUFFIX}",
					operation      : 'webContent.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: ARTICLE_BASE_NAME],
						[
							name : 'groupIds',
							from : 'steps.createSite.items[0].groupId'
						],
						[name: 'createContentsType', value: 0],
						[name: 'baseArticle', value: 'Workflow HTTP e2e body'],
						[name: 'folderId', value: 0]
					]
				]
			]
		]
	}

}
