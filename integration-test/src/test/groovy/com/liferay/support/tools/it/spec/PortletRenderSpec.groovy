package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.RequestOptions

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class PortletRenderSpec extends BaseLiferaySpec {

	private static final String NEW_PASSWORD = 'Test12345'
	private static final String PORTLET_ID = 'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'

	@Shared
	PlaywrightLifecycle pw

	@Shared
	List<String> jsErrors = []

	def setupSpec() {
		ensureDeployed()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
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

	def 'Portlet renders without JavaScript errors'() {
		given:
		Page page = pw.page

		page.onConsoleMessage(msg -> {
			if (msg.type() == 'error') {
				jsErrors.add(msg.text())
			}
		})

		page.onPageError(error -> {
			jsErrors.add(error)
		})

		when:
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()

		then: 'React component renders'
		page.locator('#num1').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('#num1').isVisible()

		and: 'no critical JavaScript errors in console'
		jsErrors.findAll {
			it.contains('ERR_ABORTED') ||
			it.contains('not supported') ||
			it.contains('Failed to fetch dynamically imported module') ||
			it.contains('404')
		}.empty
	}

	def 'ESM bundle loads from __liferay__ path'() {
		when:
		def responseCode = httpGet(
			"${liferay.baseUrl}/o/liferay-dummy-factory/__liferay__/index.js"
		)

		then:
		responseCode == 200
	}

	def 'React external resolves from Liferay runtime'() {
		when:
		def responseCode = httpGet(
			"${liferay.baseUrl}/o/frontend-js-react-web/__liferay__/exports/react.js"
		)

		then:
		responseCode == 200
	}

	private static int httpGet(String url) {
		def connection = new URL(url).openConnection() as HttpURLConnection

		connection.requestMethod = 'GET'
		connection.connectTimeout = 10_000
		connection.readTimeout = 10_000

		return connection.responseCode
	}

}
