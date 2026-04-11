package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class CompanyFunctionalSpec extends BaseLiferaySpec {

	private static final int COMPANY_COUNT = 1
	private static final String COMPANY_WEB_ID = 'ittestco'
	private static final String COMPANY_VIRTUAL_HOSTNAME = 'ittestco.example.com'
	private static final String COMPANY_MX = 'ittestco.example.com'

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long createdCompanyId

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		// Prime admin credentials so JSON-WS calls can authenticate.
		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		// CompanyService is excluded from JSON-WS by Liferay's default
		// json.service.invalid.class.names, so there is no remote delete path.
		// The Testcontainers instance is not reused (withReuse(false)), so the
		// created company is discarded with the container at the end of the run.
		pw?.close()
	}

	def 'Company is created via portlet UI'() {
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

		and: 'select Company entity type'
		page.locator('.nav-link:text-is("company")').click()

		and: 'wait for Company form to render'
		page.locator('.sheet-header h2:text-is("company")').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the company form'
		page.locator('#count').fill("${COMPANY_COUNT}")
		page.locator('#webId').fill(COMPANY_WEB_ID)
		page.locator('#virtualHostname').fill(COMPANY_VIRTUAL_HOSTNAME)
		page.locator('#mx').fill(COMPANY_MX)

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success, .alert-danger').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created company is visible via JSON-WS'() {
		when:
		def company = jsonwsGet(
			"/api/jsonws/company/get-company-by-web-id" +
			"/web-id/${COMPANY_WEB_ID}") as Map

		then:
		company != null
		company.webId == COMPANY_WEB_ID

		when:
		createdCompanyId = company.companyId as Long

		then:
		createdCompanyId != null
		createdCompanyId > 0
	}

}
