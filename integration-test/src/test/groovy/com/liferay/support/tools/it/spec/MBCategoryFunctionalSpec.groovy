package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class MBCategoryFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(MBCategoryFunctionalSpec)

	private static final String BASE_MB_CATEGORY_NAME = 'ITMBCat'
	private static final int MB_CATEGORY_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	List<Long> createdCategoryIds = []

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so that headless API
		// calls can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		// Discover Guest site groupId for MB category creation.
		def group = jsonwsGet(
			"/portal/api/jsonws/group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long
	}

	def cleanupSpec() {
		createdCategoryIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-delivery/v1.0/message-board-sections/${id}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up MB category {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'MB categories are created via portlet UI and verified via headless delivery API'() {
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

		and: 'select MB Categories entity type'
		page.locator('[data-testid="entity-selector-MB_CATEGORY"]').click()

		and: 'wait for MB Categories form to render'
		page.locator('[data-testid="mb-category-submit"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="mb-category-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the MB categories form'
		page.locator('[data-testid="mb-category-count-input"]').fill("${MB_CATEGORY_COUNT}")
		page.locator('[data-testid="mb-category-base-name-input"]').fill(BASE_MB_CATEGORY_NAME)
		page.locator('[data-testid="mb-category-group-id-select"]').selectOption("${guestGroupId}")

		and: 'click Run button'
		page.locator('[data-testid="mb-category-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="mb-category-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="mb-category-result"].alert-success').isVisible()

		when: 'query headless delivery API for created MB categories'
		def response = headlessGet(
			"/o/headless-delivery/v1.0/sites/${guestGroupId}" +
			"/message-board-sections" +
			'?pageSize=100')

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.title as String)?.startsWith(BASE_MB_CATEGORY_NAME)
		}

		createdCategoryIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created MB categories are found by title prefix'
		matching.size() == MB_CATEGORY_COUNT
	}

}
