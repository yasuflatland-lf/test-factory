package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class MBThreadFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(MBThreadFunctionalSpec)

	private static final String BASE_THREAD_NAME = 'ITMBThread'
	private static final String PREREQ_SECTION_TITLE = 'IT Prereq Section'
	private static final int THREAD_COUNT = 2

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	long prereqCategoryId

	@Shared
	List<Long> createdThreadIds = []

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so that headless API
		// calls can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		// Discover Guest site groupId for the prereq MB category.
		def group = jsonwsGet(
			"/api/jsonws/group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long

		// Create the prerequisite MB category (called "section" in the
		// headless-delivery API). The returned id matches the underlying
		// MBCategory primary key used by the portlet's categoryId dropdown.
		Map section = headlessPost(
			"/o/headless-delivery/v1.0/sites/${guestGroupId}" +
			'/message-board-sections',
			"{\"title\":\"${PREREQ_SECTION_TITLE}\"}")

		prereqCategoryId = section.id as Long

		log.info(
			'Created prereq MB section id={} title={}',
			prereqCategoryId, section.title)
	}

	def cleanupSpec() {
		// Deleting the MB section cascades to its threads in Liferay, so
		// per-thread deletes are best-effort and tolerated to fail.
		createdThreadIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-threads/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up MB thread {}: {}', id, e.message)
			}
		}

		if (prereqCategoryId > 0) {
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-sections" +
					"/${prereqCategoryId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up prereq MB section {}: {}',
					prereqCategoryId, e.message)
			}
		}

		pw?.close()
	}

	def 'creates multiple MB threads under a category via UI and verifies via headless API'() {
		given:
		Page page = pw.page

		when: 'navigate to portlet'
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()

		and: 'select MB Threads entity type'
		page.locator('[data-testid="entity-selector-MB_THREAD"]').click()

		and: 'wait for MB Threads form to render'
		page.locator('[data-testid="mb-thread-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill count and baseName'
		page.locator('[data-testid="mb-thread-count-input"]').fill("${THREAD_COUNT}")
		page.locator('[data-testid="mb-thread-base-name-input"]').fill(BASE_THREAD_NAME)

		and: 'select Guest site which triggers category dropdown load'
		page.locator('[data-testid="mb-thread-group-id-select"]').selectOption("${guestGroupId}")

		and: 'wait for the prereq category option to be attached to the category select'
		// Options inside a collapsed <select> are considered hidden by
		// default, so waiting with the default "visible" state fails.
		page.locator(
			"[data-testid=\"mb-thread-category-id-select\"] option[value=\"${prereqCategoryId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)

		and: 'select prereq category'
		page.locator('[data-testid="mb-thread-category-id-select"]').selectOption("${prereqCategoryId}")

		and: 'ensure body textarea has a non-empty value'
		page.locator('[data-testid="mb-thread-body-textarea"]').fill('This is a test message.')

		and: 'click Run button'
		page.locator('[data-testid="mb-thread-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="mb-thread-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="mb-thread-result"].alert-success').isVisible()

		when: 'query headless delivery API for created threads'
		def response = headlessGet(
			"/o/headless-delivery/v1.0/message-board-sections" +
			"/${prereqCategoryId}/message-board-threads" +
			"?search=${URLEncoder.encode(BASE_THREAD_NAME, 'UTF-8')}" +
			'&pageSize=100')

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.headline as String)?.startsWith(BASE_THREAD_NAME) ||
				(item.title as String)?.startsWith(BASE_THREAD_NAME)
		}

		createdThreadIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created MB threads are found by name prefix'
		matching.size() == THREAD_COUNT
	}

}
