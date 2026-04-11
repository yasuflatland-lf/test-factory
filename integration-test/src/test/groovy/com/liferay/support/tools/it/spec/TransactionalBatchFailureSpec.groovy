package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

import groovy.json.JsonOutput

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Locks in the per-iteration transaction contract for batch creation:
 * when one iteration inside a batch fails and is rolled back, the
 * iterations that ran BEFORE (and AFTER) the failing one must remain
 * committed in the database.
 *
 * Strategy: D (fallback). A deterministic non-skip mid-batch hard
 * failure was not found in the available experimentation window
 * (BatchNaming appends a uniform " N" suffix so length-based
 * OrganizationNameException cannot be triggered mid-batch, and reserved
 * / invalid-character names fail on iteration 1). Instead, this spec
 * exercises the "duplicate-skip" path: an organization matching the
 * batch's second iteration name is pre-created via the headless REST
 * API, forcing the Creator's iteration 2 to throw
 * DuplicateOrganizationException, which the Creator catches and
 * rolls back via the per-item TransactionInvokerUtil.invoke.
 *
 * Under the OLD whole-batch transaction contract, rolling back
 * iteration 2 would also roll back iteration 1 -- no "TxBatch 1"
 * would survive. Under the NEW per-iteration contract, iteration 1
 * commits independently and survives iteration 2's rollback, and
 * iteration 3 proceeds in its own transaction. This spec asserts
 * both iteration 1 and iteration 3 are present, which is only
 * achievable if each iteration has its own transaction boundary.
 *
 * TODO: replace with a true non-skip hard-failure trigger (e.g.,
 * one that raises a non-duplicate exception like
 * OrganizationParentException mid-batch) once such a trigger is
 * identified, for a stronger guarantee on the error path.
 */
@Stepwise
class TransactionalBatchFailureSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		TransactionalBatchFailureSpec)

	private static final String BASE_ORG_NAME = 'TxBatchOrg'
	private static final int ORG_COUNT = 3

	@Shared
	PlaywrightLifecycle pw

	@Shared
	Long preExistingOrgId

	@Shared
	List<Long> createdOrganizationIds = []

	def setupSpec() {
		ensureBundleActive()
		pw = new PlaywrightLifecycle()
	}

	def cleanupSpec() {
		createdOrganizationIds.each { id ->
			try {
				jsonwsPost(
					'/api/jsonws/organization/delete-organization',
					['organizationId': id])
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up organization {}: {}', id, e.message)
			}
		}

		if (preExistingOrgId) {
			try {
				jsonwsPost(
					'/api/jsonws/organization/delete-organization',
					['organizationId': preExistingOrgId])
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up pre-existing organization {}: {}',
					preExistingOrgId, e.message)
			}
		}

		pw?.close()
	}

	def 'Login to Liferay as admin'() {
		expect:
		loginAsAdmin(pw)
	}

	def 'Pre-create the organization that will collide with iteration 2'() {
		given:
		String collidingName = "${BASE_ORG_NAME} 2"

		when: 'pre-create an organization matching iteration 2 of the batch'
		def orgResult = headlessPost(
			'/o/headless-admin-user/v1.0/organizations',
			JsonOutput.toJson([name: collidingName])
		)
		preExistingOrgId = orgResult.id as Long
		log.info(
			'Pre-created colliding organization: id={}, name={}',
			preExistingOrgId, orgResult.name)

		then:
		preExistingOrgId != null
		(orgResult.name as String) == collidingName
	}

	def 'Batch of 3 organizations runs through a mid-batch collision'() {
		given:
		Page page = pw.page

		when: 'navigate to the dummy factory portlet'
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

		and: 'fill in the organization batch form'
		page.locator('#count').fill("${ORG_COUNT}")
		page.locator('#baseName').fill(BASE_ORG_NAME)

		and: 'submit the batch'
		page.locator('.sheet-footer button.btn-primary').click()

		then: 'portlet finishes with an alert (success or partial)'
		page.locator('.alert-success, .alert-danger').waitFor(
			new Locator.WaitForOptions().setTimeout(30_000)
		)
	}

	def 'Iteration 1 committed independently of iteration 2 rollback'() {
		when: 'query organizations via JSONWS'
		def orgs = jsonwsGet(
			"/api/jsonws/organization/get-organizations/company-id/${companyId}" +
			'/parent-organization-id/0/start/-1/end/-1') as List

		then:
		orgs != null

		when:
		def matchingItems = orgs.findAll { org ->
			(org.name as String).startsWith(BASE_ORG_NAME)
		}
		def matchingNames = matchingItems.collect { it.name as String }.sort()

		def newlyCreated = matchingItems.findAll { org ->
			(org.organizationId as Long) != preExistingOrgId
		}

		createdOrganizationIds.addAll(
			newlyCreated.collect { it.organizationId as Long }
		)

		log.info('Matching organizations after batch: {}', matchingNames)

		then: 'iteration 1 survived iteration 2 rollback'
		matchingNames.contains("${BASE_ORG_NAME} 1" as String)

		and: 'iteration 3 committed after iteration 2 rollback'
		matchingNames.contains("${BASE_ORG_NAME} 3" as String)

		and: 'the pre-existing iteration 2 row is still the original one'
		def collision = matchingItems.find { org ->
			(org.name as String) == "${BASE_ORG_NAME} 2"
		}
		collision != null
		(collision.organizationId as Long) == preExistingOrgId

		and: 'exactly two new organizations were committed by the batch'
		newlyCreated.size() == 2
	}

}
