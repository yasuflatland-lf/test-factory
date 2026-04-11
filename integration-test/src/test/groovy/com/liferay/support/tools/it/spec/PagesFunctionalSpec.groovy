package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class PagesFunctionalSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(PagesFunctionalSpec)

	private static final String BASE_PAGE_NAME = 'IT Test Page'
	private static final int PAGE_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long guestGroupId

	@Shared
	List<Long> createdPlids = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdPlids.each { plid ->
			try {
				jsonwsPost(
					'/api/jsonws/layout/delete-layout',
					['plid': plid, 'serviceContext': '{}'])
			}
			catch (Exception e) {
				log.warn('Failed to clean up layout {}: {}', plid, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Discover Guest site groupId'() {
		when:
		def group = jsonwsGet(
			"/api/jsonws/group/get-group/company-id/${companyId}" +
			'/group-key/Guest') as Map

		then:
		group != null
		group.groupId != null

		when:
		guestGroupId = group.groupId as Long

		then:
		guestGroupId > 0
	}

	def 'Pages are created via portlet UI'() {
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

		and: 'select Pages entity type'
		page.locator('.nav-link:has-text("pages")').click()

		and: 'wait for Pages form to render'
		page.locator('.sheet-header h2:has-text("pages")').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)
		page.locator('#count').waitFor(
			new Locator.WaitForOptions().setTimeout(15_000)
		)

		and: 'fill in the pages form'
		page.locator('#count').fill("${PAGE_COUNT}")
		page.locator('#baseName').fill(BASE_PAGE_NAME)
		page.locator('#groupId').selectOption("${guestGroupId}")

		and: 'click Run button'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'success alert appears'
		page.locator('.alert-success').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
		page.locator('.alert-success').isVisible()
	}

	def 'Created pages are visible via JSONWS LayoutService'() {
		when:
		def layouts = jsonwsGet(
			"/api/jsonws/layout/get-layouts/group-id/${guestGroupId}" +
			'/private-layout/false') as List

		then:
		layouts != null

		when:
		def matchingLayouts = layouts.findAll { layout ->
			def name = (layout.nameCurrentValue ?: layout.name) as String
			name?.startsWith(BASE_PAGE_NAME)
		}

		createdPlids.addAll(
			matchingLayouts.collect { it.plid as Long }
		)

		then: 'all created pages are found by name prefix'
		matchingLayouts.size() == PAGE_COUNT
	}

	def 'Test pages are cleaned up via JSONWS LayoutService'() {
		when: 'delete each created layout'
		createdPlids.each { plid ->
			jsonwsPost(
				'/api/jsonws/layout/delete-layout',
				['plid': plid, 'serviceContext': '{}'])
		}

		and: 'list layouts again'
		def layouts = jsonwsGet(
			"/api/jsonws/layout/get-layouts/group-id/${guestGroupId}" +
			'/private-layout/false') as List

		then: 'none of the test pages remain'
		!layouts.any { layout ->
			def name = (layout.nameCurrentValue ?: layout.name) as String
			name?.startsWith(BASE_PAGE_NAME)
		}
	}

}
