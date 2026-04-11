package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.JsonwsSetupHelper
import com.liferay.support.tools.it.util.LdfResourceClient

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class SiteCreationSpec extends BaseLiferaySpec {

	private static final int TYPE_SITE_OPEN = 1

	private static final int TYPE_SITE_PRIVATE = 3

	private static final String RUN_SUFFIX = String.valueOf(System.currentTimeMillis())

	private static final String BASE_OPEN_SITE = "ITOpenSite${RUN_SUFFIX}"
	private static final String BASE_PRIVATE_SITE = "ITPrivateSite${RUN_SUFFIX}"
	private static final String BASE_PROTOTYPE_SITE = "ITProtoSite${RUN_SUFFIX}"
	private static final String BASE_CHILD_SITE = "ITChildSite${RUN_SUFFIX}"

	private static final Logger log = LoggerFactory.getLogger(SiteCreationSpec)

	@Shared
	LdfResourceClient ldf

	@Shared
	JsonwsSetupHelper jsonws

	@Shared
	List<Long> createdSiteIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient("http://localhost:${liferay.httpPort}")
		jsonws = new JsonwsSetupHelper("http://localhost:${liferay.httpPort}")
	}

	def cleanupSpec() {
		createdSiteIds.each { Long id ->
			try {
				jsonwsPost(
					'/api/jsonws/group/delete-group',
					['groupId': id])
			}
			catch (Exception e) {
				log.warn('Failed to clean up site {}: {}', id, e.message)
			}
		}

		jsonws?.cleanupAll()
	}

	def 'creates sites with basic fields and open membership'() {
		given:
		Map fields = [
			count: 3,
			baseName: BASE_OPEN_SITE,
			membershipType: 'open',
			active: true,
			manualMembership: true
		]

		when: 'POST /ldf/site with open membership'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 3
		(response.sites as List)?.size() == 3

		when: 'collect created site ids for JSONWS verification'
		List<Long> groupIds = (response.sites as List).collect {
			(it as Map).groupId as Long
		}
		createdSiteIds.addAll(groupIds)

		then: 'every created group exists and is an open site'
		groupIds.every { Long groupId ->
			Map group = jsonwsGet(
				"/api/jsonws/group/get-group/group-id/${groupId}") as Map
			(group?.type as Integer) == TYPE_SITE_OPEN
		}
	}

	def 'creates sites with private membership type'() {
		given:
		Map fields = [
			count: 2,
			baseName: BASE_PRIVATE_SITE,
			membershipType: 'private',
			active: true,
			manualMembership: true
		]

		when: 'POST /ldf/site with private membership'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 2

		when: 'collect created site ids for JSONWS verification'
		List<Long> groupIds = (response.sites as List).collect {
			(it as Map).groupId as Long
		}
		createdSiteIds.addAll(groupIds)

		then: 'every created group is a private site'
		groupIds.every { Long groupId ->
			Map group = jsonwsGet(
				"/api/jsonws/group/get-group/group-id/${groupId}") as Map
			(group?.type as Integer) == TYPE_SITE_PRIVATE
		}
	}

	def 'links public and private layout set prototypes to created sites'() {
		given: 'pre-create one public and one private layout set prototype'
		Map publicProto = jsonws.createLayoutSetPrototype(
			"ITPubProto${RUN_SUFFIX}", false)
		Map privateProto = jsonws.createLayoutSetPrototype(
			"ITPrivProto${RUN_SUFFIX}", true)

		Long publicProtoId = publicProto.layoutSetPrototypeId as Long
		Long privateProtoId = privateProto.layoutSetPrototypeId as Long

		and: 'fetch prototype uuids via JSONWS'
		Map publicProtoDetail = jsonwsGet(
			"/api/jsonws/layoutsetprototype/get-layout-set-prototype" +
			"/layout-set-prototype-id/${publicProtoId}") as Map
		Map privateProtoDetail = jsonwsGet(
			"/api/jsonws/layoutsetprototype/get-layout-set-prototype" +
			"/layout-set-prototype-id/${privateProtoId}") as Map

		String expectedPublicUuid = publicProtoDetail.uuid as String
		String expectedPrivateUuid = privateProtoDetail.uuid as String

		Map fields = [
			count: 1,
			baseName: BASE_PROTOTYPE_SITE,
			membershipType: 'open',
			active: true,
			manualMembership: true,
			publicLayoutSetPrototypeId: publicProtoId,
			privateLayoutSetPrototypeId: privateProtoId
		]

		when: 'POST /ldf/site with both prototype ids'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 1

		when: 'collect created site id'
		Long createdGroupId = ((response.sites as List).first() as Map).groupId as Long
		createdSiteIds << createdGroupId

		and: 'fetch public and private layout sets via JSONWS'
		Map publicLayoutSet = jsonwsGet(
			"/api/jsonws/layoutset/get-layout-set" +
			"/group-id/${createdGroupId}/private-layout/false") as Map
		Map privateLayoutSet = jsonwsGet(
			"/api/jsonws/layoutset/get-layout-set" +
			"/group-id/${createdGroupId}/private-layout/true") as Map

		then: 'each layout set is linked to the pre-created prototype uuid'
		(publicLayoutSet?.layoutSetPrototypeUuid as String) == expectedPublicUuid
		(privateLayoutSet?.layoutSetPrototypeUuid as String) == expectedPrivateUuid
	}

	def 'inherits content from parent site when inheritContent=true'() {
		given: 'pre-create a parent site and one journal article in it'
		Map parentSite = jsonws.createSite(
			"ITParentSite${RUN_SUFFIX}", 'open')
		Long parentGroupId = parentSite.groupId as Long

		jsonws.createJournalArticle(
			parentGroupId,
			"ITParentArticle${RUN_SUFFIX}",
			'<?xml version="1.0"?>' +
				'<root available-locales="en_US" default-locale="en_US">' +
				'<static-content language-id="en_US">' +
				'<![CDATA[<p>parent article body</p>]]>' +
				'</static-content></root>')

		int parentArticleCount = jsonwsGet(
			"/api/jsonws/journal.journalarticle/get-articles-count" +
			"/group-id/${parentGroupId}/folder-id/0") as Integer

		assert parentArticleCount >= 1

		Map fields = [
			count: 1,
			baseName: BASE_CHILD_SITE,
			membershipType: 'open',
			active: true,
			manualMembership: true,
			parentGroupId: parentGroupId,
			inheritContent: true
		]

		when: 'POST /ldf/site with parentGroupId and inheritContent=true'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 1

		when: 'collect created child site id'
		Long childGroupId = ((response.sites as List).first() as Map).groupId as Long
		createdSiteIds << childGroupId

		and: 'fetch the child group via JSONWS'
		Map childGroup = jsonwsGet(
			"/api/jsonws/group/get-group/group-id/${childGroupId}") as Map

		then: 'child group records parent relationship and inheritContent flag'
		(childGroup?.parentGroupId as Long) == parentGroupId
		(childGroup?.inheritContent as Boolean) == true
	}

}
