package com.liferay.test.factory.it.spec

import com.liferay.test.factory.it.container.LiferayContainer
import com.liferay.test.factory.it.util.PlaywrightLifecycle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class CalculatorHappyPathSpec extends BaseLiferaySpec {

	private static final String PORTLET_ID =
		'com_liferay_test_factory_TestFactoryPortlet'

	// New password to satisfy the change-required policy
	private static final String NEW_PASSWORD = 'Test12345'

	@Shared
	PlaywrightLifecycle pw

	@Shared
	String currentPassword = LiferayContainer.DEFAULT_ADMIN_PASSWORD

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
		page.navigate("${liferay.baseUrl}/c/portal/login")
		page.waitForLoadState()

		when:
		def loginForm = page.locator('form:has(#_com_liferay_login_web_portlet_LoginPortlet_login)')
		loginForm.locator('#_com_liferay_login_web_portlet_LoginPortlet_login')
			.fill(LiferayContainer.DEFAULT_ADMIN_EMAIL)
		loginForm.locator('#_com_liferay_login_web_portlet_LoginPortlet_password')
			.fill(currentPassword)
		page.waitForNavigation({ ->
			loginForm.locator('[type=submit], button.btn-primary').first().click()
		})
		println "After login: ${page.title()} -> ${page.url()}"

		// Handle "New Password" page
		if (page.title().contains('New Password')) {
			page.locator('#password1').fill(NEW_PASSWORD)
			page.locator('#password2').fill(NEW_PASSWORD)
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
			currentPassword = NEW_PASSWORD
			println "After password change: ${page.title()} -> ${page.url()}"
		}

		// Handle "Password Reminder" page
		if (page.title().contains('Password Reminder') || page.locator('#reminderQueryAnswer').isVisible()) {
			page.locator('#reminderQueryAnswer').fill('test')
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
			println "After reminder: ${page.title()} -> ${page.url()}"
		}

		then:
		page.title().contains('Home') || page.title().contains('Welcome')
		!page.title().contains('New Password')
	}

	def 'Navigate to Calculator in Control Panel'() {
		given:
		Page page = pw.page

		when:
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage?" +
			"p_p_id=${PORTLET_ID}&p_p_lifecycle=0&p_p_state=maximized"
		)
		page.waitForLoadState()
		println "Control Panel title: ${page.title()}"
		println "Control Panel URL: ${page.url()}"
		println "HTML snippet: ${page.content().take(1000)}"

		then:
		page.locator('#num1').waitFor(new Locator.WaitForOptions().setTimeout(10_000))
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
