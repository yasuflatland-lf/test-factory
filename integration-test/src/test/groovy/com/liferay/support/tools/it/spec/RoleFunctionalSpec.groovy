package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class RoleFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(RoleFunctionalSpec)

	private static final String BASE_ROLE_NAME = 'ITTestRole'
	private static final int ROLE_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<Long> createdRoleIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		try {
			def result = headlessGet('/o/headless-admin-user/v1.0/roles?pageSize=200')

			result.items?.findAll { (it.name as String).startsWith(BASE_ROLE_NAME) }
				?.each { item ->
					try {
						headlessDelete("/o/headless-admin-user/v1.0/roles/${item.id}")
					}
					catch (Exception e) {
						log.warn('Failed to clean up role {}: {}', item.id, e.message)
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

	def 'Roles are created via portlet UI'() {
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

		and: 'select Roles entity type'
		page.locator('.nav-link:has-text("roles")').click()
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the role form'
		page.locator('#count').fill("${ROLE_COUNT}")
		page.locator('#baseName').fill(BASE_ROLE_NAME)

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created roles are visible via headless REST API'() {
		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/roles?pageSize=200')

		then:
		result.items != null

		when:
		def matchingItems = result.items.findAll { item ->
			(item.name as String).startsWith(BASE_ROLE_NAME)
		}

		createdRoleIds.addAll(
			matchingItems.collect { it.id as Long }
		)

		then: 'all created roles exist'
		matchingItems.size() == ROLE_COUNT
		matchingItems.collect { it.name }.sort() ==
			(1..ROLE_COUNT).collect { "${BASE_ROLE_NAME}${it}" }
	}

	def 'Test roles are cleaned up via headless REST API'() {
		when:
		def deleteResults = createdRoleIds.collect { id ->
			headlessDelete("/o/headless-admin-user/v1.0/roles/${id}")
		}

		then: 'all deletes succeed'
		deleteResults.every { it in [200, 204] }

		when:
		def result = headlessGet('/o/headless-admin-user/v1.0/roles?pageSize=200')

		then: 'none of the test roles remain'
		!result.items?.any { item ->
			(item.name as String).startsWith(BASE_ROLE_NAME)
		}
	}

}
