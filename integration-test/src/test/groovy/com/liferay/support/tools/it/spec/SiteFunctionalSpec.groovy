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
	String apiResponseBody = ''

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
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

		and: 'capture API response and click Run'
		page.onResponse(response -> {
			try {
				String body = response.text()

				if (body?.contains('"sites"')) {
					apiResponseBody = body
				}
			}
			catch (ignored) {}
		})

		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success, .alert-danger').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'API response confirms sites were created'() {
		expect:
		log.info('API response: {}', apiResponseBody)
		apiResponseBody.contains('"success":true')
		apiResponseBody.contains("\"count\":${SITE_COUNT}")

		and: 'response contains expected site names'
		(1..SITE_COUNT).every { i ->
			apiResponseBody.contains("\"name\":\"${BASE_SITE_NAME}${i}\"")
		}
	}

}
