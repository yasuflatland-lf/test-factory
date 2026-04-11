package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class VocabularyFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(VocabularyFunctionalSpec)

	private static final String BASE_VOCAB_NAME = 'ITVocab'
	private static final int VOCAB_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	long guestGroupId

	@Shared
	List<Long> createdVocabularyIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdVocabularyIds.each { id ->
			try {
				headlessDelete(
					"/o/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up vocabulary {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Discover Guest site groupId'() {
		when:
		def group = jsonwsGet(
			"/api/jsonws/group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		then:
		group != null
		group.groupId != null

		when:
		guestGroupId = group.groupId as Long

		then:
		guestGroupId > 0
	}

	def 'Vocabularies are created via portlet UI'() {
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

		and: 'select Vocabularies entity type'
		page.locator('.nav-link:has-text("vocabularies")').click()

		and: 'wait for Vocabularies form to render'
		page.locator('.sheet-header h2:has-text("vocabularies")').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the vocabularies form'
		page.locator('#count').fill("${VOCAB_COUNT}")
		page.locator('#baseName').fill(BASE_VOCAB_NAME)
		page.locator('#groupId').selectOption("${guestGroupId}")

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created vocabularies are visible via headless taxonomy API'() {
		when:
		def response = headlessGet(
			"/o/headless-admin-taxonomy/v1.0/sites/${guestGroupId}" +
			"/taxonomy-vocabularies?search=${URLEncoder.encode(BASE_VOCAB_NAME, 'UTF-8')}" +
			'&pageSize=100')

		then:
		response != null
		response.items != null

		when:
		def items = response.items as List
		def matching = items.findAll { item ->
			(item.name as String)?.startsWith(BASE_VOCAB_NAME)
		}

		createdVocabularyIds.addAll(
			matching.collect { it.id as Long }
		)

		then: 'all created vocabularies are found by name prefix'
		matching.size() == VOCAB_COUNT
	}

}
