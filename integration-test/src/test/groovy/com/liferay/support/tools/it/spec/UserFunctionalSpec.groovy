package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class UserFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(UserFunctionalSpec)

	private static final String BASE_USER_NAME = 'ITTestUser'
	private static final int USER_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdUserIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		try {
			def result = headlessGet('/o/headless-admin-user/v1.0/user-accounts?pageSize=100')
			String prefix = BASE_USER_NAME.toLowerCase()

			result.items?.findAll { (it.alternateName as String).startsWith(prefix) }
				?.each { item ->
					try {
						headlessDelete("/o/headless-admin-user/v1.0/user-accounts/${item.id}")
					}
					catch (Exception e) {
						log.warn('Failed to clean up user {}: {}', item.id, e.message)
					}
				}
		}
		catch (Exception e) {
			log.warn('Fallback cleanup failed: {}', e.message)
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Users are created via portlet UI'() {
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

		and: 'select Users entity type'
		page.locator('.nav-link:has-text("users")').click()
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the user form'
		page.locator('#count').fill("${USER_COUNT}")
		page.locator('#baseName').fill(BASE_USER_NAME)

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created users are visible via headless REST API'() {
		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/user-accounts?pageSize=100')

		then:
		result.items != null

		when:
		String expectedPrefix = BASE_USER_NAME.toLowerCase()

		def matchingItems = result.items.findAll { item ->
			(item.alternateName as String).startsWith(expectedPrefix)
		}

		createdUserIds.addAll(
			matchingItems.collect { it.id as Long }
		)

		then: 'all created users exist with expected screen names'
		matchingItems.size() == USER_COUNT
		matchingItems.collect { it.alternateName }.sort() ==
			(1..USER_COUNT).collect { "${expectedPrefix}${it}" }
		matchingItems.collect { it.emailAddress }.sort() ==
			(1..USER_COUNT).collect { "${expectedPrefix}${it}@liferay.com" }
	}

	def 'Test users are cleaned up via headless REST API'() {
		when:
		def deleteResults = createdUserIds.collect { id ->
			headlessDelete("/o/headless-admin-user/v1.0/user-accounts/${id}")
		}

		then: 'all deletes succeed'
		deleteResults.every { it in [200, 204] }

		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/user-accounts?pageSize=100')

		then: 'none of the test users remain'
		String expectedPrefix = BASE_USER_NAME.toLowerCase()

		!result.items?.any { item ->
			(item.alternateName as String).startsWith(expectedPrefix)
		}
	}

}
