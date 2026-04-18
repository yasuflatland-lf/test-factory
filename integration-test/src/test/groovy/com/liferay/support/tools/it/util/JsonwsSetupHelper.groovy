package com.liferay.support.tools.it.util

import groovy.json.JsonSlurper

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Spock-friendly helper that creates (and later deletes) precondition data
 * via Liferay JSONWS so that tests can assume a specific pre-state.
 *
 * Every create* method tracks the created record so {@link #cleanupAll()}
 * can delete everything in reverse insertion order. Exceptions raised during
 * cleanup are logged but never rethrown, so a failing teardown never masks
 * the real test failure.
 */
class JsonwsSetupHelper {

	/** Matches {@code com.liferay.portal.kernel.model.role.RoleConstants#TYPE_REGULAR}. */
	public static final int ROLE_TYPE_REGULAR = 1

	private static final String NEW_PASSWORD = 'Test12345'

	private static final Logger _log = LoggerFactory.getLogger(JsonwsSetupHelper)

	private final String _baseUrl
	private final String _username
	private String _authHeader
	private final List<String> _candidatePasswords
	private final List<Tracked> _tracked = []

	JsonwsSetupHelper(
		String baseUrl, String username = 'test@liferay.com',
		String password = 'test') {

		_baseUrl = baseUrl.endsWith('/') ? baseUrl[0..-2] : baseUrl
		_username = username
		_candidatePasswords = [password, NEW_PASSWORD].findAll { it != null }.unique()
		_authHeader = _basicAuthFor(password)
	}

	private String _basicAuthFor(String password) {
		return 'Basic ' + "${_username}:${password}".bytes.encodeBase64().toString()
	}

	Map createRole(String name, int type = ROLE_TYPE_REGULAR) {
		Map response = _post(
			'/api/jsonws/role/add-role',
			[
				'externalReferenceCode': '',
				'className': 'com.liferay.portal.kernel.model.Role',
				'classPK': '0',
				'name': name,
				'titleMap': _localizedJson(name),
				'descriptionMap': '{}',
				'type': type,
				'subtype': ''
			]) as Map

		_tracked << new Tracked(
			'role', '/api/jsonws/role/delete-role', 'roleId',
			response.roleId as Long)

		return response
	}

	Map createOrganization(String name) {
		// OrganizationService.addOrganization takes a ServiceContext which
		// the JSONWS form encoder cannot serialize, so every form-encoded
		// POST to /api/jsonws/organization/add-organization returns 404
		// regardless of overload. Route through the headless-admin-user
		// REST API instead, which uses a clean JSON body and a single
		// "organizations" endpoint.
		String body = "{\"name\":${_jsonQuote(name)}}"
		Map response = _postJson(
			'/o/headless-admin-user/v1.0/organizations', body) as Map

		Long organizationId = response.id as Long

		// The headless response nests the created entity; normalize the shape
		// so callers see a consistent JSONWS-style "organizationId" key.
		response.organizationId = organizationId

		_tracked << new Tracked(
			'organization', '/api/jsonws/organization/delete-organization',
			'organizationId', organizationId)

		return response
	}

	private static String _jsonQuote(String value) {
		String escaped = (value ?: '')
			.replace('\\', '\\\\')
			.replace('"', '\\"')

		return "\"${escaped}\""
	}

	private Object _postJson(String path, String jsonBody) {
		int lastStatus = 0
		String lastResponseBody = ''

		for (String password : _candidatePasswords) {
			String authHeader = _basicAuthFor(password)

			def conn = new URL(_baseUrl + path).openConnection() as HttpURLConnection

			try {
				conn.requestMethod = 'POST'
				conn.connectTimeout = 10_000
				conn.readTimeout = 30_000
				conn.setRequestProperty('Authorization', authHeader)
				conn.setRequestProperty('Accept', 'application/json')
				conn.setRequestProperty('Content-Type', 'application/json')
				conn.doOutput = true

				conn.outputStream.withWriter('UTF-8') { writer ->
					writer.write(jsonBody)
				}

				int status = conn.responseCode
				String responseBody = (status < 400)
					? (conn.inputStream?.text ?: '')
					: (conn.errorStream?.text ?: '')

				lastStatus = status
				lastResponseBody = responseBody

				if ((status == 401) || (status == 403)) {
					continue
				}

				if (status >= 400) {
					throw new IllegalStateException(
						"Headless POST ${path} returned HTTP ${status}: " +
							responseBody)
				}

				_authHeader = authHeader

				if (!responseBody?.trim() || responseBody.trim() == 'null') {
					return null
				}

				return new JsonSlurper().parseText(responseBody)
			}
			finally {
				conn.disconnect()
			}
		}

		throw new IllegalStateException(
			"Headless POST ${path} returned HTTP ${lastStatus} for all " +
				"candidate passwords: ${lastResponseBody}")
	}

	Map createSite(String name, String membershipType = 'open') {
		int type = _siteTypeToConstant(membershipType)

		Map response = _post(
			'/api/jsonws/group/add-group',
			[
				'externalReferenceCode': '',
				'parentGroupId': '0',
				'liveGroupId': '0',
				'nameMap': _localizedJson(name),
				'descriptionMap': _localizedJson(''),
				'type': type,
				'typeSettings': '',
				'manualMembership': 'true',
				'membershipRestriction': '0',
				'friendlyURL': '/' + name.toLowerCase().replaceAll(/[^a-z0-9]+/, '-'),
				'site': 'true',
				'inheritContent': 'false',
				'active': 'true',
				'serviceContext': '{}'
			]) as Map

		_tracked << new Tracked(
			'group', '/api/jsonws/group/delete-group', 'groupId',
			response.groupId as Long)

		return response
	}

	Map createLayoutSetPrototype(String name, boolean privateLayout = false) {
		Map response = _post(
			'/api/jsonws/layoutsetprototype/add-layout-set-prototype',
			[
				'nameMap': _localizedJson(name),
				'descriptionMap': _localizedJson(''),
				'active': 'true',
				'layoutsUpdateable': 'true',
				'serviceContext': '{}'
			]) as Map

		_tracked << new Tracked(
			'layoutSetPrototype',
			'/api/jsonws/layoutsetprototype/delete-layout-set-prototype',
			'layoutSetPrototypeId',
			response.layoutSetPrototypeId as Long)

		return response
	}

	Map createDdmStructure(long groupId, String name, String definitionJson) {
		Map response = _post(
			'/api/jsonws/ddm/ddmstructure/add-structure',
			[
				'groupId': groupId,
				'parentStructureId': '0',
				'classNameId': _classNameIdFor(
					'com.liferay.journal.model.JournalArticle'),
				'structureKey': name,
				'nameMap': _localizedJson(name),
				'descriptionMap': _localizedJson(''),
				'definition': definitionJson,
				'storageType': 'json',
				'type': '0'
			]) as Map

		_tracked << new Tracked(
			'ddmStructure', '/api/jsonws/ddm/ddmstructure/delete-structure',
			'structureId',
			response.structureId as Long)

		return response
	}

	Map createDdmTemplate(
		long groupId, long structureId, String name, String script) {

		Map response = _post(
			'/api/jsonws/ddm/ddmtemplate/add-template',
			[
				'groupId': groupId,
				'classNameId': _classNameIdFor(
					'com.liferay.dynamic.data.mapping.model.DDMStructure'),
				'classPK': structureId,
				'resourceClassNameId': _classNameIdFor(
					'com.liferay.journal.model.JournalArticle'),
				'templateKey': name,
				'nameMap': _localizedJson(name),
				'descriptionMap': _localizedJson(''),
				'type': 'display',
				'mode': '',
				'language': 'ftl',
				'script': script,
				'cacheable': 'true'
			]) as Map

		_tracked << new Tracked(
			'ddmTemplate', '/api/jsonws/ddm/ddmtemplate/delete-template',
			'templateId',
			response.templateId as Long)

		return response
	}

	Map createJournalArticle(long groupId, String title, String content) {
		Map response = _post(
			'/api/jsonws/journal/journalarticle/add-article',
			[
				'groupId': groupId,
				'folderId': '0',
				'classNameId': '0',
				'classPK': '0',
				'articleId': '',
				'autoArticleId': 'true',
				'titleMapAsXML': _titleMapXml(title),
				'descriptionMapAsXML': _titleMapXml(title),
				'content': content,
				'ddmStructureKey': 'BASIC-WEB-CONTENT',
				'ddmTemplateKey': 'BASIC-WEB-CONTENT'
			]) as Map

		_tracked << new Tracked(
			'journalArticle',
			'/api/jsonws/journal/journalarticle/delete-article',
			'articleId', response.articleId as String,
			['groupId': groupId, 'articleId': response.articleId as String])

		return response
	}

	/**
	 * Deletes every record created by this helper instance in reverse order of
	 * insertion. Cleanup errors are logged and swallowed so a failing teardown
	 * never hides the original test failure.
	 */
	void cleanupAll() {
		for (int i = _tracked.size() - 1; i >= 0; i--) {
			Tracked entry = _tracked[i]

			try {
				_post(entry.deletePath, entry.deleteParams())
			}
			catch (Exception e) {
				_log.warn(
					'JsonwsSetupHelper cleanup failed for {} (pk={}): {}',
					entry.kind, entry.primaryKey, e.message)
			}
		}

		_tracked.clear()
	}

	private static String _localizedJson(String value) {
		String escaped = value
			.replace('\\', '\\\\')
			.replace('"', '\\"')

		return /{"en_US":"${escaped}"}/
	}

	private static String _titleMapXml(String value) {
		String escaped = value
			.replace('&', '&amp;')
			.replace('<', '&lt;')
			.replace('>', '&gt;')

		return '<?xml version="1.0"?>' +
			'<root available-locales="en_US" default-locale="en_US">' +
			"<Title language-id=\"en_US\">${escaped}</Title></root>"
	}

	private static int _siteTypeToConstant(String membershipType) {
		switch (membershipType?.toLowerCase()) {
			case 'open': return 1
			case 'restricted': return 2
			case 'private': return 3
			default:
				throw new IllegalArgumentException(
					"Unknown site membership type: ${membershipType}")
		}
	}

	private String _classNameIdFor(String className) {
		def response = _get(
			"/api/jsonws/classname/fetch-class-name/value/" +
				URLEncoder.encode(className, 'UTF-8')) as Map

		if ((response == null) || (response.classNameId == null)) {
			throw new IllegalStateException(
				"Could not resolve classNameId for ${className}")
		}

		return response.classNameId.toString()
	}

	private Object _get(String path) {
		int lastStatus = 0
		String lastResponseBody = ''

		for (String password : _candidatePasswords) {
			String authHeader = _basicAuthFor(password)

			def conn = new URL(_baseUrl + path).openConnection() as HttpURLConnection

			try {
				conn.requestMethod = 'GET'
				conn.connectTimeout = 10_000
				conn.readTimeout = 30_000
				conn.setRequestProperty('Authorization', authHeader)
				conn.setRequestProperty('Accept', 'application/json')

				int status = conn.responseCode
				String body = (status < 400)
					? (conn.inputStream?.text ?: '')
					: (conn.errorStream?.text ?: '')

				lastStatus = status
				lastResponseBody = body

				if ((status == 401) || (status == 403)) {
					continue
				}

				if (status >= 400) {
					throw new IllegalStateException(
						"JSONWS GET ${path} returned HTTP ${status}: ${body}")
				}

				_authHeader = authHeader

				if (!body?.trim() || body.trim() == 'null') {
					return null
				}

				return new JsonSlurper().parseText(body)
			}
			finally {
				conn.disconnect()
			}
		}

		throw new IllegalStateException(
			"JSONWS GET ${path} returned HTTP ${lastStatus} for all " +
				"candidate passwords: ${lastResponseBody}")
	}

	private Object _post(String path, Map<String, Object> params) {
		String body = params.collect { k, v ->
			"${URLEncoder.encode(k as String, 'UTF-8')}=" +
				"${URLEncoder.encode(v == null ? '' : v.toString(), 'UTF-8')}"
		}.join('&')

		int lastStatus = 0
		String lastResponseBody = ''

		for (String password : _candidatePasswords) {
			String authHeader = _basicAuthFor(password)

			def conn = new URL(_baseUrl + path).openConnection() as HttpURLConnection

			try {
				conn.requestMethod = 'POST'
				conn.connectTimeout = 10_000
				conn.readTimeout = 30_000
				conn.setRequestProperty('Authorization', authHeader)
				conn.setRequestProperty('Accept', 'application/json')
				conn.setRequestProperty(
					'Content-Type', 'application/x-www-form-urlencoded')
				conn.doOutput = true

				conn.outputStream.withWriter('UTF-8') { writer ->
					writer.write(body)
				}

				int status = conn.responseCode
				String responseBody = (status < 400)
					? (conn.inputStream?.text ?: '')
					: (conn.errorStream?.text ?: '')

				lastStatus = status
				lastResponseBody = responseBody

				// 401/403 with this password may mean the admin's password has
				// been rotated (e.g. by the Playwright login flow in
				// LdfResourceClient). Retry with the other candidate password.
				if ((status == 401) || (status == 403)) {
					continue
				}

				if (status >= 400) {
					throw new IllegalStateException(
						"JSONWS POST ${path} returned HTTP ${status}: ${responseBody}")
				}

				// Remember the password that actually worked so subsequent
				// calls go straight to it.
				_authHeader = authHeader

				if (!responseBody?.trim() || responseBody.trim() == 'null') {
					return null
				}

				return new JsonSlurper().parseText(responseBody)
			}
			finally {
				conn.disconnect()
			}
		}

		throw new IllegalStateException(
			"JSONWS POST ${path} returned HTTP ${lastStatus} for all " +
				"candidate passwords: ${lastResponseBody}")
	}

	private static class Tracked {

		final String kind
		final String deletePath
		final String primaryKeyParam
		final Object primaryKey
		final Map<String, Object> extraParams

		Tracked(
			String kind, String deletePath, String primaryKeyParam,
			Object primaryKey, Map<String, Object> extraParams = null) {

			this.kind = kind
			this.deletePath = deletePath
			this.primaryKeyParam = primaryKeyParam
			this.primaryKey = primaryKey
			this.extraParams = extraParams
		}

		Map<String, Object> deleteParams() {
			if (extraParams != null) {
				return extraParams
			}

			return [(primaryKeyParam): primaryKey]
		}

	}

}
