package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import groovy.json.JsonOutput

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class UserRoleAssignmentSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(UserRoleAssignmentSpec)

	private static final String BASE_USER_NAME = 'ITRoleUser'
	private static final String TEST_ORG_NAME = 'ITRoleTestOrg'
	private static final String USERS_ADMIN_URL_PATH =
		'/group/guest/~/control_panel/manage' +
		'?p_p_id=com_liferay_users_admin_web_portlet_UsersAdminPortlet' +
		'&p_p_lifecycle=0' +
		'&p_p_state=maximized'

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long testOrgId

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		if (testOrgId) {
			try {
				headlessDelete("/o/headless-admin-user/v1.0/organizations/${testOrgId}")
			}
			catch (Exception e) {
				log.warn('Failed to clean up organization {}: {}', testOrgId, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Create test organization for role assignment'() {
		when: 'create fresh organization'
		def orgResult = headlessPost(
			'/o/headless-admin-user/v1.0/organizations',
			JsonOutput.toJson([name: TEST_ORG_NAME])
		)
		testOrgId = orgResult.id as Long
		log.info('Created test organization: id={}, name={}', testOrgId, orgResult.name)

		then:
		testOrgId != null
	}

	def 'User is created with organization assignment via portlet UI'() {
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

		and: 'fill in user creation fields'
		page.locator('#count').fill('1')
		page.locator('#baseName').fill(BASE_USER_NAME)

		and: 'expand advanced options'
		page.locator('button.btn-link:has-text("advanced")').click()
		page.locator('#organizationIds').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'select the test organization in the multiselect'
		page.locator("#organizationIds option:has-text(\"${TEST_ORG_NAME}\")").click()

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created user exists in Users and Organizations'() {
		given:
		Page page = pw.page
		String expectedScreenName = BASE_USER_NAME.toLowerCase() + '1'

		when: 'navigate to Users and Organizations'
		page.navigate("${liferay.baseUrl}${USERS_ADMIN_URL_PATH}")
		page.waitForLoadState()

		and: 'search for the test user'
		def searchInput = page.locator('input[type="text"].form-control, input.search-bar-input, input[name="keywords"]').first()
		searchInput.waitFor(new Locator.WaitForOptions().setTimeout(15_000))
		searchInput.fill(expectedScreenName)
		searchInput.press('Enter')
		page.waitForLoadState()

		and: 'wait for results table'
		page.locator('table tbody tr').first().waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)

		then: 'the user appears in the results'
		def rows = page.locator('table tbody tr')
		boolean found = false
		for (int i = 0; i < rows.count(); i++) {
			String text = rows.nth(i).textContent().toLowerCase()
			if (text.contains(expectedScreenName)) {
				found = true
				break
			}
		}
		log.info('User {} found in Users and Organizations: {}', expectedScreenName, found)
		found
	}

}
