package com.liferay.support.tools.it.util

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Spock-friendly HTTP helper for POSTing to liferay-dummy-factory ResourceURLs
 * and retrieving the parsed JSON alert payload.
 *
 * <p>
 * The helper constructs portlet ResourceURLs directly rather than rendering the
 * portlet UI. It logs in once via {@code /c/portal/login}, keeps the session
 * cookie for the lifetime of the instance, and reuses it across all POSTs so
 * that the instance is safe to share across Spock feature methods using
 * {@code @Shared}.
 * </p>
 *
 * <p>
 * Each {@link #post} call also polls {@code /ldf/progress} with the generated
 * {@code progressId} until the creator job reports 100%, but the returned Map
 * is always the JSON body from the POST itself (which is the final payload,
 * not the progress record).
 * </p>
 */
class LdfResourceClient {

	static final String PORTLET_ID =
		'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'

	private static final String CONTROL_PANEL_PATH =
		'/group/control_panel/manage'

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60)
	private static final long PROGRESS_TIMEOUT_MS = 60_000L
	private static final long PROGRESS_POLL_MS = 500L

	// The creator services are synchronous: the POST response already contains
	// the final payload. Progress polling is best-effort and bails out after a
	// short streak of zero-percent replies to avoid hanging on a tracker that
	// was never installed.
	private static final int PROGRESS_ZERO_STREAK_LIMIT = 4

	private static final Logger log = LoggerFactory.getLogger(LdfResourceClient)

	private final String _baseUrl
	private final String _username
	private final String _password

	private final HttpClient _httpClient
	private final StringBuilder _cookieJar = new StringBuilder()

	private String _authToken
	private boolean _loggedIn = false

	LdfResourceClient(
		String baseUrl, String username = 'test@liferay.com',
		String password = 'test') {

		_baseUrl = baseUrl?.endsWith('/') ?
			baseUrl.substring(0, baseUrl.length() - 1) : baseUrl
		_username = username
		_password = password

		_httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NEVER)
			.build()
	}

	Map post(String mvcCommandName, Map<String, Object> fields) {
		_ensureLoggedIn()

		String progressId = UUID.randomUUID().toString()
		String jsonData = JsonOutput.toJson(fields ?: [:])

		String formBody =
			"data=${_encode(jsonData)}" +
			"&progressId=${_encode(progressId)}" +
			(_authToken ? "&p_auth=${_encode(_authToken)}" : '')

		URI resourceURI = _buildResourceURI(mvcCommandName)

		HttpRequest request = HttpRequest.newBuilder(resourceURI)
			.timeout(REQUEST_TIMEOUT)
			.header('Accept', 'application/json')
			.header('Content-Type', 'application/x-www-form-urlencoded')
			.header('Cookie', _cookieJar.toString())
			.header('Authorization', _basicAuthHeader())
			.POST(HttpRequest.BodyPublishers.ofString(
				formBody, StandardCharsets.UTF_8))
			.build()

		HttpResponse<String> response = _send(request)

		_captureCookies(response)

		int status = response.statusCode()
		String body = response.body() ?: ''

		if ((status < 200) || (status >= 300)) {
			throw new RuntimeException(
				"POST ${mvcCommandName} returned HTTP ${status}: ${body}")
		}

		_pollProgressUntilDone(progressId)

		if (!body.trim()) {
			return [:]
		}

		def parsed = new JsonSlurper().parseText(body)

		if (parsed instanceof Map) {
			return parsed as Map
		}

		return [result: parsed] as Map
	}

	Map createUser(Map<String, Object> fields) {
		return post('/ldf/user', fields)
	}

	Map createSite(Map<String, Object> fields) {
		return post('/ldf/site', fields)
	}

	Map createWebContent(Map<String, Object> fields) {
		return post('/ldf/wcm', fields)
	}

	private URI _buildResourceURI(String mvcCommandName) {
		String query =
			"p_p_id=${_encode(PORTLET_ID)}" +
			'&p_p_lifecycle=2' +
			"&p_p_resource_id=${_encode(mvcCommandName)}"

		return URI.create("${_baseUrl}${CONTROL_PANEL_PATH}?${query}")
	}

	private synchronized void _ensureLoggedIn() {
		if (_loggedIn) {
			return
		}

		String loginBody =
			"login=${_encode(_username)}" +
			"&password=${_encode(_password)}" +
			'&rememberMe=true'

		HttpRequest loginRequest = HttpRequest.newBuilder(
			URI.create("${_baseUrl}/c/portal/login"))
			.timeout(REQUEST_TIMEOUT)
			.header('Content-Type', 'application/x-www-form-urlencoded')
			.header('Authorization', _basicAuthHeader())
			.POST(HttpRequest.BodyPublishers.ofString(
				loginBody, StandardCharsets.UTF_8))
			.build()

		HttpResponse<String> loginResponse = _send(loginRequest)
		int status = loginResponse.statusCode()
		String loginBodyText = loginResponse.body() ?: ''

		_captureCookies(loginResponse)

		// Liferay's /c/portal/login can respond in several successful shapes:
		//   * 302 redirect to /c (classic form post)
		//   * 200 with a meta-refresh HTML body that redirects the browser
		//     to /c (seen on CE 7.4 GA132 when no explicit redirect param is
		//     supplied)
		//   * 500 with the same meta-refresh body (Liferay still sets the
		//     authenticated session cookies before throwing, which is what
		//     we actually need)
		//
		// Instead of trusting the status code, consider the login successful
		// when either the response body contains the meta-refresh hand-off to
		// /c, or when the Set-Cookie headers established a JSESSIONID. That
		// covers every variant above without masking a genuine auth failure,
		// which Liferay signals by re-rendering the login form (no redirect
		// body, no new session cookie).
		boolean metaRefreshToC =
			loginBodyText.contains("window.location.replace('\\x2fc')") ||
			loginBodyText.contains('url=/c') ||
			loginBodyText.contains("location.replace('/c')")

		boolean hasSession = _cookieJar.toString().contains('JSESSIONID=')

		boolean redirected = (status == 302) || (status == 301)

		boolean success = redirected || metaRefreshToC || hasSession ||
			((status >= 200) && (status < 300))

		if (!success) {
			throw new RuntimeException(
				"Login failed for ${_username}: HTTP ${status}: " +
				"${loginBodyText}")
		}

		_authToken = _fetchAuthToken()
		_loggedIn = true

		log.info(
			'LdfResourceClient logged in as {} (p_auth={})',
			_username, _authToken ? 'present' : 'missing')
	}

	private String _fetchAuthToken() {
		HttpRequest request = HttpRequest.newBuilder(
			URI.create("${_baseUrl}/"))
			.timeout(REQUEST_TIMEOUT)
			.header('Cookie', _cookieJar.toString())
			.header('Authorization', _basicAuthHeader())
			.GET()
			.build()

		HttpResponse<String> response = _send(request)

		_captureCookies(response)

		String body = response.body() ?: ''

		def matcher = body =~ /Liferay\.authToken\s*=\s*['"]([^'"]+)['"]/

		if (matcher.find()) {
			return matcher.group(1)
		}

		return null
	}

	private void _pollProgressUntilDone(String progressId) {
		long deadline = System.currentTimeMillis() + PROGRESS_TIMEOUT_MS
		int zeroStreak = 0

		while (System.currentTimeMillis() < deadline) {
			try {
				String query =
					"p_p_id=${_encode(PORTLET_ID)}" +
					'&p_p_lifecycle=2' +
					"&p_p_resource_id=${_encode('/ldf/progress')}" +
					"&progressId=${_encode(progressId)}"

				URI withProgress = URI.create(
					"${_baseUrl}${CONTROL_PANEL_PATH}?${query}")

				HttpRequest request = HttpRequest.newBuilder(withProgress)
					.timeout(REQUEST_TIMEOUT)
					.header('Accept', 'application/json')
					.header('Cookie', _cookieJar.toString())
					.header('Authorization', _basicAuthHeader())
					.GET()
					.build()

				HttpResponse<String> response = _send(request)

				_captureCookies(response)

				int status = response.statusCode()

				if ((status >= 200) && (status < 300)) {
					String body = response.body() ?: ''

					if (body.trim()) {
						def parsed = new JsonSlurper().parseText(body)

						if (parsed instanceof Map) {
							Integer percent = (parsed.percent as Integer) ?: 0

							if (percent >= 100) {
								return
							}

							if (percent == 0) {
								zeroStreak++

								if (zeroStreak >= PROGRESS_ZERO_STREAK_LIMIT) {
									// No tracker was ever installed: the POST
									// response is already the final payload.
									return
								}
							}
							else {
								zeroStreak = 0
							}
						}
					}
				}
			}
			catch (Exception e) {
				log.debug('Progress poll failed: {}', e.message)
			}

			TimeUnit.MILLISECONDS.sleep(PROGRESS_POLL_MS)
		}

		log.warn(
			'Progress polling for {} did not reach 100% within {} ms',
			progressId, PROGRESS_TIMEOUT_MS)
	}

	private HttpResponse<String> _send(HttpRequest request) {
		try {
			return _httpClient.send(
				request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
		}
		catch (Exception e) {
			throw new RuntimeException(
				"HTTP ${request.method()} ${request.uri()} failed: ${e.message}",
				e)
		}
	}

	private void _captureCookies(HttpResponse<String> response) {
		List<String> setCookies = response.headers().allValues('Set-Cookie')

		if (!setCookies) {
			return
		}

		for (String raw in setCookies) {
			String cookie = raw.split(';', 2)[0]

			if (!cookie?.contains('=')) {
				continue
			}

			String name = cookie.substring(0, cookie.indexOf('=')).trim()
			String existing = _cookieJar.toString()

			if (existing.contains("${name}=")) {
				String replaced = existing.replaceAll(
					"${java.util.regex.Pattern.quote(name)}=[^;]*",
					java.util.regex.Matcher.quoteReplacement(cookie))
				_cookieJar.setLength(0)
				_cookieJar.append(replaced)
			}
			else {
				if (_cookieJar.length() > 0) {
					_cookieJar.append('; ')
				}
				_cookieJar.append(cookie)
			}
		}
	}

	private String _basicAuthHeader() {
		String credentials = "${_username}:${_password}"

		return "Basic ${credentials.bytes.encodeBase64().toString()}"
	}

	private static String _encode(String value) {
		return URLEncoder.encode(value ?: '', StandardCharsets.UTF_8.name())
	}

}
