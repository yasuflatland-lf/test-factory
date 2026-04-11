package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class CompanyFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(CompanyFunctionalSpec)

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
		if (createdCompanyId != null) {
			try {
				jsonwsPost(
					'/api/jsonws/company/delete-company',
					[companyId: createdCompanyId])
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up company {}: {}',
					createdCompanyId, e.message)
			}
		}

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
