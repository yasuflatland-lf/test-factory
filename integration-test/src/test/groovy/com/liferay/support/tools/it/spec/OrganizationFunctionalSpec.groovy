package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.RequestOptions

import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class OrganizationFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(OrganizationFunctionalSpec)

	private static final String PORTLET_ID = 'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'
	private static final String NEW_PASSWORD = 'Test12345'
	private static final String BASE_ORG_NAME = 'IT Test Org'
	private static final int ORG_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	String activePassword = LiferayContainer.DEFAULT_ADMIN_PASSWORD

	@Shared
	List<Long> createdOrganizationIds = []

	def setupSpec() {
		ensureDeployed()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdOrganizationIds.each { id ->
			try {
				headlessDelete("/o/headless-admin-user/v1.0/organizations/${id}")
			}
			catch (Exception ignored) {
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		given:
		Page page = pw.newPage()

		when:
		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		String authToken = page.evaluate('() => Liferay.authToken') as String

		def passwords = [LiferayContainer.DEFAULT_ADMIN_PASSWORD, NEW_PASSWORD]
		boolean loggedIn = false

		for (pwd in passwords) {
			def response = page.request().post("${liferay.baseUrl}/c/portal/login",
				RequestOptions.create()
					.setHeader('Content-Type', 'application/x-www-form-urlencoded')
					.setHeader('x-csrf-token', authToken)
					.setData("login=${URLEncoder.encode(LiferayContainer.DEFAULT_ADMIN_EMAIL, 'UTF-8')}&password=${URLEncoder.encode(pwd, 'UTF-8')}&rememberMe=true")
			)

			if (response.status() == 200) {
				activePassword = pwd
				loggedIn = true
				break
			}
		}

		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		if (page.title().contains('New Password')) {
			page.locator('#password1').fill(NEW_PASSWORD)
			page.locator('#password2').fill(NEW_PASSWORD)
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
			activePassword = NEW_PASSWORD
		}

		if (page.locator('#reminderQueryAnswer').isVisible()) {
			page.locator('#reminderQueryAnswer').fill('test')
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
		}

		then:
		loggedIn
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

	// -- Headless REST API helpers --

	private Map headlessGet(String path) {
		def conn = new URL("${liferay.baseUrl}${path}").openConnection() as HttpURLConnection

		conn.requestMethod = 'GET'
		conn.connectTimeout = 10_000
		conn.readTimeout = 10_000
		conn.setRequestProperty('Authorization', basicAuthHeader())
		conn.setRequestProperty('Accept', 'application/json')

		int status = conn.responseCode
		String body = (status < 400) ? conn.inputStream.text : (conn.errorStream?.text ?: '')

		if (status >= 400) {
			throw new IllegalStateException("headlessGet ${path} returned HTTP ${status}: ${body}")
		}

		return new JsonSlurper().parseText(body) as Map
	}

	private int headlessDelete(String path) {
		def conn = new URL("${liferay.baseUrl}${path}").openConnection() as HttpURLConnection

		conn.requestMethod = 'DELETE'
		conn.connectTimeout = 10_000
		conn.readTimeout = 10_000
		conn.setRequestProperty('Authorization', basicAuthHeader())

		return conn.responseCode
	}

	private String basicAuthHeader() {
		String credentials =
			"${LiferayContainer.DEFAULT_ADMIN_EMAIL}:${activePassword}"

		return "Basic ${credentials.bytes.encodeBase64().toString()}"
	}

}
