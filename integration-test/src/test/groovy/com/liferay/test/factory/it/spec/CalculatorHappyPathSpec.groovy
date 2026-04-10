package com.liferay.test.factory.it.spec

import com.liferay.test.factory.it.container.LiferayContainer
import com.liferay.test.factory.it.util.PlaywrightLifecycle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.RequestOptions

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class CalculatorHappyPathSpec extends BaseLiferaySpec {

	private static final String NEW_PASSWORD = 'Test12345'

	@Shared
	PlaywrightLifecycle pw

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
		// 1. Navigate to get session + CSRF token (Liferay's performLoginViaApi pattern)
		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		String authToken = page.evaluate('() => Liferay.authToken') as String
		println "CSRF token: ${authToken}"

		// 2. POST login with CSRF token
		def passwords = [LiferayContainer.DEFAULT_ADMIN_PASSWORD, NEW_PASSWORD]
		boolean loggedIn = false

		for (pwd in passwords) {
			def response = page.request().post("${liferay.baseUrl}/c/portal/login",
				RequestOptions.create()
					.setHeader('Content-Type', 'application/x-www-form-urlencoded')
					.setHeader('x-csrf-token', authToken)
					.setData("login=${URLEncoder.encode(LiferayContainer.DEFAULT_ADMIN_EMAIL, 'UTF-8')}&password=${URLEncoder.encode(pwd, 'UTF-8')}&rememberMe=true")
			)
			println "Login with '${pwd}': HTTP ${response.status()}"

			if (response.status() == 200) {
				loggedIn = true
				break
			}
		}

		// 3. Reload to pick up session
		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()
		println "After login reload: ${page.title()} -> ${page.url()}"

		// 4. Handle "New Password" page
		if (page.title().contains('New Password')) {
			page.locator('#password1').fill(NEW_PASSWORD)
			page.locator('#password2').fill(NEW_PASSWORD)
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
			println "After password change: ${page.title()} -> ${page.url()}"
		}

		// 5. Handle "Password Reminder" page
		if (page.locator('#reminderQueryAnswer').isVisible()) {
			page.locator('#reminderQueryAnswer').fill('test')
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
		}

		println "Final: ${page.title()} -> ${page.url()}"

		then:
		loggedIn
	}

	def 'Navigate to Calculator via direct URL'() {
		given:
		Page page = pw.page
		def portletId = 'com_liferay_test_factory_TestFactoryPortlet'

		when:
		// Navigate directly to the portlet using Control Panel URL pattern
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${portletId}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()
		println "Calculator page: ${page.title()} -> ${page.url()}"

		then:
		page.locator('#num1').waitFor(new Locator.WaitForOptions().setTimeout(15_000))
		page.locator('#num1').isVisible()
	}

	def 'Happy path: 10 + 5 = 15'() {
		expect:
		calculateAndVerify(pw.page, '10', '+', '5', '15')
	}

	def 'Happy path: 20 / 4 = 5'() {
		expect:
		calculateAndVerify(pw.page, '20', '/', '4', '5')
	}

	private static boolean calculateAndVerify(
		Page page, String a, String op, String b, String expected) {

		page.locator('#num1').fill(a)
		page.locator('#operator').selectOption(op)
		page.locator('#num2').fill(b)
		page.locator('button.btn-primary').click()

		def resultLocator = page.locator('.alert-success')

		resultLocator.waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		return resultLocator.textContent().contains(expected)
	}

}
