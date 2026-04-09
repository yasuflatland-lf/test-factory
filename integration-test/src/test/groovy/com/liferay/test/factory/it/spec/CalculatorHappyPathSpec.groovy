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
		page.navigate("${liferay.baseUrl}/c/portal/login")

		when:
		page.locator('#_com_liferay_login_web_portlet_LoginPortlet_login')
			.fill(LiferayContainer.DEFAULT_ADMIN_EMAIL)
		page.locator('#_com_liferay_login_web_portlet_LoginPortlet_password')
			.fill(LiferayContainer.DEFAULT_ADMIN_PASSWORD)
		page.locator('[type=submit]').first().click()

		then:
		page.waitForURL(
			'**/web/guest/**',
			new Page.WaitForURLOptions().setTimeout(30_000)
		)
	}

	def 'Navigate to Calculator in Control Panel'() {
		given:
		Page page = pw.page

		when:
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage?" +
			"p_p_id=${PORTLET_ID}&p_p_lifecycle=0"
		)
		page.waitForLoadState()

		then:
		page.locator('#num1').isVisible()
		page.locator('#num2').isVisible()
		page.locator('#operator').isVisible()
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
