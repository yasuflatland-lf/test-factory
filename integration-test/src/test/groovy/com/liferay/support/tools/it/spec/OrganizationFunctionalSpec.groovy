package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class OrganizationFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(OrganizationFunctionalSpec)

	private static final String BASE_ORG_NAME = 'IT Test Org'
	private static final int ORG_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdOrganizationIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdOrganizationIds.each { id ->
			try {
				headlessDelete("/o/headless-admin-user/v1.0/organizations/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up organization {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Organizations are created via portlet UI'() {
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

		and: 'wait for the form to render'
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the organization form'
		page.locator('#count').fill("${ORG_COUNT}")
		page.locator('#baseName').fill(BASE_ORG_NAME)

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created organizations are visible via headless REST API'() {
		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/organizations?pageSize=100')

		then:
		result.items != null

		when:
		def matchingItems = result.items.findAll { item ->
			(item.name as String).startsWith(BASE_ORG_NAME)
		}

		createdOrganizationIds.addAll(
			matchingItems.collect { it.id as Long }
		)

		then: 'all created organizations exist with expected names'
		matchingItems.size() == ORG_COUNT
		matchingItems.collect { it.name }.sort() == (1..ORG_COUNT).collect { "${BASE_ORG_NAME} ${it}" }
	}

	def 'Test organizations are cleaned up via headless REST API'() {
		when:
		def deleteResults = createdOrganizationIds.collect { id ->
			headlessDelete("/o/headless-admin-user/v1.0/organizations/${id}")
		}

		then: 'all deletes succeed'
		deleteResults.every { it in [200, 204] }

		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/organizations?pageSize=100')

		then: 'none of the test organizations remain'
		!result.items?.any { item ->
			(item.name as String).startsWith(BASE_ORG_NAME)
		}
	}

}
