package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class SiteFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(SiteFunctionalSpec)

	private static final String BASE_SITE_NAME = 'ITTestSite'
	private static final int SITE_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdSiteIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdSiteIds.each { id ->
			try {
				headlessDelete("/o/headless-admin-user/v1.0/sites/${id}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up site {}: {}', id, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Sites are created via portlet UI'() {
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

		and: 'select Sites entity type'
		page.locator('.nav-link:has-text("sites")').click()

		and: 'wait for Sites form to render'
		page.locator('.sheet-header h2:has-text("sites")').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the site form'
		page.locator('#count').fill("${SITE_COUNT}")
		page.locator('#baseName').fill(BASE_SITE_NAME)

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created sites are visible via headless REST API'() {
		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/sites?page=1&pageSize=200')

		then:
		result.items != null

		when:
		def matchingItems = result.items.findAll { item ->
			(item.name as String).startsWith(BASE_SITE_NAME)
		}

		createdSiteIds.addAll(
			matchingItems.collect { it.id as Long }
		)

		then: 'all created sites exist with expected names'
		matchingItems.size() == SITE_COUNT
		matchingItems.collect { it.name }.sort() == (1..SITE_COUNT).collect { "${BASE_SITE_NAME}${it}" }
	}

	def 'Test sites are cleaned up via headless REST API'() {
		when:
		def deleteResults = createdSiteIds.collect { id ->
			headlessDelete("/o/headless-admin-user/v1.0/sites/${id}")
		}

		then: 'all deletes succeed'
		deleteResults.every { it in [200, 204] }

		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/sites?page=1&pageSize=200')

		then: 'none of the test sites remain'
		!result.items?.any { item ->
			(item.name as String).startsWith(BASE_SITE_NAME)
		}
	}

}
