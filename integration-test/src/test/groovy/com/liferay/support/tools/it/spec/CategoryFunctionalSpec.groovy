package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitForSelectorState

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class CategoryFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(CategoryFunctionalSpec)

	private static final String BASE_CATEGORY_NAME = 'ITCategory'
	private static final String PREREQ_VOCAB_NAME = 'IT Prereq Vocab'
	private static final int CATEGORY_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	long prereqVocabularyId

	@Shared
	List<Long> createdCategoryIds = []

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin password via Playwright login so that headless API
		// calls can authenticate with the active credentials immediately.
		loginAsAdmin(pw)

		// Discover Guest site groupId for the prereq vocabulary.
		def group = jsonwsGet(
			"/portal/api/jsonws/group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		guestGroupId = group.groupId as Long

		// Create the prerequisite vocabulary via Headless Admin Taxonomy API.
		Map vocab = headlessPost(
			"/o/headless-admin-taxonomy/v1.0/sites/${guestGroupId}" +
			'/taxonomy-vocabularies',
			"{\"name\":\"${PREREQ_VOCAB_NAME}\"}")

		prereqVocabularyId = vocab.id as Long

		log.info(
			'Created prereq vocabulary id={} name={}',
			prereqVocabularyId, vocab.name)
	}

	def cleanupSpec() {
		createdCategoryIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-categories/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up category {}: {}', id, e.message)
			}
		}

		if (prereqVocabularyId > 0) {
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" +
					"/${prereqVocabularyId}")
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up prereq vocabulary {}: {}',
					prereqVocabularyId, e.message)
			}
		}

		pw?.close()
	}

	def 'creates multiple categories under vocabulary via UI and verifies via headless API'() {
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

		and: 'select Categories entity type'
		page.locator('[data-testid="entity-selector-CATEGORY"]').click()

		and: 'wait for Categories form to render'
		page.locator('[data-testid="category-submit"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('[data-testid="category-count-input"]').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill count and baseName'
		page.locator('[data-testid="category-count-input"]').fill("${CATEGORY_COUNT}")
		page.locator('[data-testid="category-base-name-input"]').fill(BASE_CATEGORY_NAME)

		and: 'select Guest site which triggers vocabulary dropdown load'
		page.locator('[data-testid="category-group-id-select"]').selectOption("${guestGroupId}")

		and: 'wait for vocabulary dropdown to populate with prereq vocab'
		page.locator(
			"[data-testid=\"category-vocabulary-id-select\"] option[value=\"${prereqVocabularyId}\"]"
		).waitFor(
			new Locator.WaitForOptions()
				.setState(WaitForSelectorState.ATTACHED)
				.setTimeout(15_000)
		)

		and: 'select prereq vocabulary'
		page.locator('[data-testid="category-vocabulary-id-select"]').selectOption("${prereqVocabularyId}")

		and: 'click Run button'
		page.locator('[data-testid="category-submit"]').click()

		then: 'success alert appears'
		page.locator('[data-testid="category-result"].alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('[data-testid="category-result"].alert-success').isVisible()

		when: 'query headless taxonomy API for created categories'
		def response = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies" +
			"/${prereqVocabularyId}/taxonomy-categories" +
			"?search=${URLEncoder.encode(BASE_CATEGORY_NAME, 'UTF-8')}" +
			'&pageSize=100')

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.name as String)?.startsWith(BASE_CATEGORY_NAME)
		}

		createdCategoryIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created categories are found by name prefix'
		matching.size() == CATEGORY_COUNT
	}

}
