package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.GogoShellClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.RequestOptions

import groovy.json.JsonSlurper

import org.jacoco.core.tools.ExecDumpClient

import spock.lang.Shared
import spock.lang.Specification

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class BaseLiferaySpec extends Specification {

	protected static final String NEW_PASSWORD = 'Test12345'
	protected static final String PORTLET_ID = 'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'

	private static final Logger log = LoggerFactory.getLogger(BaseLiferaySpec)

	@Shared
	static LiferayContainer liferay = LiferayContainer.getInstance()

	@Shared
	static boolean bundleVerified = false

	@Shared
	static String activePassword = LiferayContainer.DEFAULT_ADMIN_PASSWORD

	@Shared
	static Long cachedCompanyId = null

	static Path getModuleJarPath() {
		Path jarDir = Path.of(System.getProperty('user.dir')).parent.resolve(
			'modules/liferay-dummy-factory/build/libs')

		File jar = jarDir.toFile().listFiles()?.find { File f ->
			f.name.endsWith('.jar') && f.name.contains('liferay.dummy.factory')
		}

		if (jar == null) {
			throw new IllegalStateException(
				"Module JAR not found in ${jarDir}. " +
				"Run './gradlew :modules:liferay-dummy-factory:build' first."
			)
		}

		return jar.toPath()
	}

	private static final String BUNDLE_SYMBOLIC_NAME = 'liferay.dummy.factory'
	private static final int BUNDLE_ACTIVATION_MAX_ATTEMPTS = 60
	private static final int BUNDLE_ACTIVATION_INTERVAL_SECONDS = 5

	@Shared
	static boolean adminBootstrapped = false

	static synchronized void ensureBundleActive() {
		bootstrapAdminCredentials()

		if (bundleVerified) {
			return
		}

		log.info('Deploying JAR: {}', getModuleJarPath())
		liferay.deployJar(getModuleJarPath())
		log.info('JAR copied to container. GoGo Shell at {}:{}', liferay.host, liferay.gogoPort)

		boolean active = false
		List<String> lastMatches = []
		String lastBundleId = null

		for (int i = 0; i < BUNDLE_ACTIVATION_MAX_ATTEMPTS; i++) {
			try {
				new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
					String output = gogo.execute('lb')
					def allLines = output.readLines()
					int lineCount = allLines.size()
					def tail = allLines.takeRight(5)
					log.info('GoGo Shell attempt {}: {} lines, last 5: {}', i + 1, lineCount, tail)
					def lines = allLines.findAll { it.toLowerCase().contains('liferay') && it.toLowerCase().contains('dummy') && it.toLowerCase().contains('factory') }
					log.info('Matches: {}', lines ?: '(no match)')
					lastMatches = lines

					if (lines) {
						def idMatcher = (lines[0] =~ /^\s*(\d+)/)
						if (idMatcher.find()) {
							lastBundleId = idMatcher.group(1)
						}
					}

					if (lines.any { it.contains('Active') }) {
						active = true
					}
				}

				if (active) {
					break
				}
			}
			catch (Exception e) {
				log.warn('GoGo Shell attempt {} failed: {}', i + 1, e.message)
			}

			TimeUnit.SECONDS.sleep(BUNDLE_ACTIVATION_INTERVAL_SECONDS)
		}

		if (!active) {
			throw new IllegalStateException(_buildActivationFailureMessage(lastMatches, lastBundleId))
		}

		bundleVerified = true
	}

	private static String _buildActivationFailureMessage(List<String> lastMatches, String lastBundleId) {
		int timeoutSeconds = BUNDLE_ACTIVATION_MAX_ATTEMPTS * BUNDLE_ACTIVATION_INTERVAL_SECONDS

		StringBuilder message = new StringBuilder()
		message.append('Bundle ').append(BUNDLE_SYMBOLIC_NAME)
		message.append(' did not reach ACTIVE state within ').append(timeoutSeconds).append('s.\n')

		String lbOutput = _safeGogoExecute("lb ${BUNDLE_SYMBOLIC_NAME}".toString())
		message.append('--- lb ').append(BUNDLE_SYMBOLIC_NAME).append(' ---\n')
		message.append(lbOutput ?: '(gogo unreachable)').append('\n')

		if (lastMatches) {
			message.append('--- last lb matches from polling loop ---\n')
			message.append(lastMatches.join('\n')).append('\n')
		}
		else {
			message.append('--- bundle never appeared in lb output during polling (not installed) ---\n')
		}

		if (lastBundleId) {
			String diagOutput = _safeGogoExecute("diag ${lastBundleId}".toString())
			message.append('--- diag ').append(lastBundleId).append(' ---\n')
			message.append(diagOutput ?: '(gogo unreachable)').append('\n')
		}

		return message.toString()
	}

	private static String _safeGogoExecute(String command) {
		try {
			return new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
				return gogo.execute(command)
			}
		}
		catch (Exception e) {
			log.warn('GoGo diagnostic command "{}" failed: {}', command, e.message)
			return null
		}
	}

	// DXP 2026 ships test@liferay.com with PASSWORDRESET=1 baked into the HSQL
	// database. Every JSONWS/Headless call returns HTTP 403 until the reset is
	// cleared. We clear it once per container lifetime via the update_password
	// ticket flow, then switch basic-auth credentials to NEW_PASSWORD.
	static synchronized void bootstrapAdminCredentials() {
		if (adminBootstrapped) {
			return
		}

		_waitForLicenseActivated()

		if (_checkBasicAuth(LiferayContainer.DEFAULT_ADMIN_PASSWORD)) {
			activePassword = LiferayContainer.DEFAULT_ADMIN_PASSWORD
			adminBootstrapped = true
			log.info('Admin bootstrap: JSONWS reachable with default password, no reset needed')
			return
		}

		if (_checkBasicAuth(NEW_PASSWORD)) {
			activePassword = NEW_PASSWORD
			adminBootstrapped = true
			log.info('Admin bootstrap: JSONWS reachable with NEW_PASSWORD (reset already cleared)')
			return
		}

		CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL)
		CookieHandler previous = CookieHandler.getDefault()
		CookieHandler.setDefault(cookieManager)

		try {
			// p_auth is a CSRF token for authenticated-state actions. The
			// login POST itself can accept requests without it on DXP 2026
			// (LoginFilter treats /c/portal/login as an anonymous public
			// path). We still try to extract one from the landing page
			// in case a DXP release starts requiring it for login; otherwise
			// we POST without.
			String pAuth = _fetchLoginPageAuth()

			_postLogin(pAuth)

			Map<String, String> ticket = _fetchUpdatePasswordTicket()
			Map<String, String> formTokens = _fetchUpdatePasswordForm(ticket)

			_postUpdatePassword(ticket, formTokens, NEW_PASSWORD)
		}
		finally {
			// Drop session cookies so the post-update Basic Auth probe is not
			// treated as the still-passwordReset session.
			CookieHandler.setDefault(previous)
		}

		// Post-update probe WITHOUT the form-login cookies. A fresh session
		// forces DXP to evaluate Basic Auth against the updated DB state
		// rather than reuse the cached passwordReset=true session.
		boolean ok = false
		for (int attempt = 0; attempt < 30; attempt++) {
			if (_checkBasicAuth(NEW_PASSWORD)) {
				ok = true
				log.info(
					'Admin bootstrap: password change propagated after {}s', attempt)
				break
			}
			TimeUnit.SECONDS.sleep(1)
		}

		if (!ok) {
			throw new IllegalStateException(
				'Admin bootstrap: update_password flow completed but Basic Auth still returns 403 after 30s (without session cookies)')
		}

		activePassword = NEW_PASSWORD
		adminBootstrapped = true
		log.info('Admin bootstrap: password reset cleared, switched to NEW_PASSWORD')
	}

	// DXP 2026 redirects every page to /c/portal/license_activation until its
	// license file is processed (~14 s after Tomcat startup). awaitLiferayReady
	// only checks TCP port 8080, which responds before the license chain is
	// registered. Poll until the login page stops redirecting to the
	// license-activation page. Cap at 120 s.
	private static void _waitForLicenseActivated() {
		for (int attempt = 0; attempt < 120; attempt++) {
			HttpURLConnection conn = null
			try {
				conn = new URL(
					"${liferay.baseUrl}/sign-in"
				).openConnection() as HttpURLConnection
				conn.requestMethod = 'GET'
				conn.connectTimeout = 5_000
				conn.readTimeout = 10_000
				conn.instanceFollowRedirects = false

				// Trigger the request; the redirect target is what we care about.
				conn.responseCode
				String location = conn.getHeaderField('Location') ?: ''

				if (!location.contains('license_activation')) {
					if (attempt > 0) {
						log.info(
							'Admin bootstrap: license activated after {}s', attempt)
					}
					return
				}
			}
			catch (IOException e) {
				log.debug('Admin bootstrap: license probe failed: {}', e.message)
			}
			finally {
				conn?.disconnect()
			}

			TimeUnit.SECONDS.sleep(1)
		}

		log.warn(
			'Admin bootstrap: license still not activated after 120s — proceeding anyway')
	}

	private static boolean _checkBasicAuth(String password) {
		HttpURLConnection conn = null

		try {
			conn = new URL(
				"${liferay.baseUrl}/api/jsonws/user/get-current-user"
			).openConnection() as HttpURLConnection
			conn.requestMethod = 'GET'
			conn.connectTimeout = 10_000
			conn.readTimeout = 15_000

			String credentials =
				"${LiferayContainer.DEFAULT_ADMIN_EMAIL}:${password}"

			conn.setRequestProperty(
				'Authorization', "Basic ${credentials.bytes.encodeBase64()}")
			conn.setRequestProperty('Accept', 'application/json')

			return conn.responseCode == 200
		}
		catch (IOException e) {
			log.warn('Admin bootstrap: basic-auth probe failed: {}', e.message)
			return false
		}
		finally {
			conn?.disconnect()
		}
	}

	private static String _fetchLoginPageAuth() {
		// Try /sign-in first, then /c, then /. On a cold container the landing
		// page varies; any authenticated-redirect landing page will contain a
		// p_auth token embedded in a form or URL.
		for (String path in ['/sign-in', '/c', '/']) {
			String body = _httpGet("${liferay.baseUrl}${path}")

			Matcher matcher = Pattern.compile(/p_auth=([A-Za-z0-9]+)/).matcher(body)

			if (matcher.find()) {
				return matcher.group(1)
			}

			// Fallback: Liferay.authToken in a JS block
			Matcher altMatcher = Pattern.compile(
				/"authToken"\s*:\s*"([A-Za-z0-9]+)"|Liferay\.authToken\s*=\s*['"]([A-Za-z0-9]+)['"]/).matcher(body)

			if (altMatcher.find()) {
				return altMatcher.group(1) ?: altMatcher.group(2)
			}

			// Dump hex of first 64 bytes for diagnostics — body may look empty
			// if it starts with whitespace/newlines or non-ASCII.
			StringBuilder hex = new StringBuilder()
			int dumpLen = Math.min(64, body?.length() ?: 0)
			for (int i = 0; i < dumpLen; i++) {
				hex.append(String.format('%02x ', (int)body.charAt(i) & 0xff))
			}

			log.warn(
				'Admin bootstrap: p_auth not found in {} (body length {}). First 64 bytes hex: {}',
				path, body?.length() ?: 0, hex.toString())
		}

		return null
	}

	private static void _postLogin(String pAuth) {
		String body =
			'login=' + URLEncoder.encode(LiferayContainer.DEFAULT_ADMIN_EMAIL, 'UTF-8') +
			'&password=' + URLEncoder.encode(LiferayContainer.DEFAULT_ADMIN_PASSWORD, 'UTF-8')

		String url = "${liferay.baseUrl}/c/portal/login"
		if (pAuth) {
			url = "${url}?p_auth=${pAuth}"
		}

		// Use _httpPostRead so we can inspect the final body (after redirect) for
		// alert-danger markers. A 302 redirect (success) is followed automatically
		// because instanceFollowRedirects=true; the final body is then checked.
		String html = _httpPostRead(url, 'application/x-www-form-urlencoded', body)

		_assertNoAlertDanger(html, 'login POST')
	}

	private static Map<String, String> _fetchUpdatePasswordTicket() {
		String body = _httpGet("${liferay.baseUrl}/c")

		String ticketId = _extractHiddenField(body, 'ticketId')
		String ticketKey = _extractHiddenField(body, 'ticketKey')

		if (!ticketId || !ticketKey) {
			throw new IllegalStateException(
				"Admin bootstrap: ticketId/ticketKey not found after login (body length=${body.length()})")
		}

		return [ticketId: ticketId, ticketKey: ticketKey]
	}

	private static Map<String, String> _fetchUpdatePasswordForm(Map<String, String> ticket) {
		String body =
			'p_l_id=0' +
			'&ticketId=' + URLEncoder.encode(ticket.ticketId, 'UTF-8') +
			'&ticketKey=' + URLEncoder.encode(ticket.ticketKey, 'UTF-8')

		String html = _httpPostRead(
			"${liferay.baseUrl}/c/portal/update_password",
			'application/x-www-form-urlencoded', body)

		Map<String, String> tokens = [:]
		['formDate', 'p_l_id', 'p_auth'].each { name ->
			String value = _extractHiddenField(html, name)
			if (value) {
				tokens[name] = value
			}
		}

		if (!tokens.p_auth) {
			throw new IllegalStateException(
				'Admin bootstrap: p_auth not found in update_password form')
		}

		return tokens
	}

	private static void _postUpdatePassword(
			Map<String, String> ticket, Map<String, String> formTokens,
			String newPassword) {

		String body = [
			'formDate=' + URLEncoder.encode(formTokens.formDate ?: '', 'UTF-8'),
			'p_l_id=' + URLEncoder.encode(formTokens.p_l_id ?: '0', 'UTF-8'),
			'p_auth=' + URLEncoder.encode(formTokens.p_auth, 'UTF-8'),
			'doAsUserId=',
			'cmd=update',
			'referer=' + URLEncoder.encode('/c', 'UTF-8'),
			'ticketId=' + URLEncoder.encode(ticket.ticketId, 'UTF-8'),
			'ticketKey=' + URLEncoder.encode(ticket.ticketKey, 'UTF-8'),
			'password1=' + URLEncoder.encode(newPassword, 'UTF-8'),
			'password2=' + URLEncoder.encode(newPassword, 'UTF-8')
		].join('&')

		// Use _httpPostRead so we can inspect the final body for alert-danger
		// markers. DXP rejects invalid passwords (policy violation, ticket mismatch,
		// same-as-old) with HTTP 200 + an alert-danger block rather than an error
		// status, so status-only checks miss these failures.
		String html = _httpPostRead(
			"${liferay.baseUrl}/c/portal/update_password",
			'application/x-www-form-urlencoded', body)

		_assertNoAlertDanger(html, 'update_password POST')
	}

	// Scans the response body for a DXP alert-danger block. DXP returns HTTP 200
	// with an inline error message when credentials are wrong, the account is
	// locked, or a password-policy check fails. Without this check the caller
	// proceeds silently and fails later with a misleading "ticketId not found"
	// error. Throws IllegalStateException with a trimmed snippet on detection.
	private static void _assertNoAlertDanger(String html, String step) {
		Matcher m = Pattern.compile(
			/class\s*=\s*"[^"]*alert-danger[^"]*"[^>]*>([\s\S]{0,400}?)</).matcher(html ?: '')

		if (m.find()) {
			String msg = m.group(1).replaceAll('<[^>]+>', ' ').replaceAll('\\s+', ' ').trim()
			throw new IllegalStateException("Admin bootstrap: ${step} failed — ${msg}")
		}
	}

	private static String _extractHiddenField(String html, String name) {
		Pattern pattern = Pattern.compile(
			/name\s*=\s*"\Q${name}\E"[^>]*value\s*=\s*"([^"]*)"|value\s*=\s*"([^"]*)"[^>]*name\s*=\s*"\Q${name}\E"/)

		Matcher matcher = pattern.matcher(html)

		if (!matcher.find()) {
			return null
		}

		return matcher.group(1) ?: matcher.group(2)
	}

	private static String _httpGet(String url) {
		HttpURLConnection conn = null

		try {
			conn = new URL(url).openConnection() as HttpURLConnection
			conn.requestMethod = 'GET'
			conn.connectTimeout = 10_000
			conn.readTimeout = 30_000
			conn.instanceFollowRedirects = true
			// Force identity encoding — HttpURLConnection does not auto-decode
			// gzip, and decoding as UTF-8 then garbles the bytes.
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.setRequestProperty(
				'Accept',
				'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8')

			int status = conn.responseCode
			InputStream in = status < 400 ? conn.inputStream : conn.errorStream

			return in == null ? '' : _readAll(in)
		}
		finally {
			conn?.disconnect()
		}
	}

	private static String _readAll(InputStream in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream()
		byte[] buf = new byte[8192]
		int n
		while ((n = in.read(buf)) > 0) {
			out.write(buf, 0, n)
		}
		return new String(out.toByteArray(), 'UTF-8')
	}

	private static String _httpPostRead(String url, String contentType, String body) {
		HttpURLConnection conn = null

		try {
			conn = new URL(url).openConnection() as HttpURLConnection
			conn.requestMethod = 'POST'
			conn.connectTimeout = 10_000
			conn.readTimeout = 30_000
			conn.instanceFollowRedirects = true
			conn.setRequestProperty('Content-Type', contentType)
			conn.setRequestProperty('Accept-Encoding', 'identity')
			conn.doOutput = true
			conn.outputStream.withWriter('UTF-8') { writer ->
				writer.write(body)
			}

			int status = conn.responseCode
			InputStream in = status < 400 ? conn.inputStream : conn.errorStream

			return in == null ? '' : _readAll(in)
		}
		finally {
			conn?.disconnect()
		}
	}

	protected static String loginAsAdmin(PlaywrightLifecycle pw) {
		Page page = pw.newPage()

		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		String authToken = page.evaluate('() => Liferay.authToken') as String

		def passwords = [LiferayContainer.DEFAULT_ADMIN_PASSWORD, NEW_PASSWORD]
		String loggedInPassword = null

		for (pwd in passwords) {
			def response = page.request().post("${liferay.baseUrl}/c/portal/login",
				RequestOptions.create()
					.setHeader('Content-Type', 'application/x-www-form-urlencoded')
					.setHeader('x-csrf-token', authToken)
					.setData("login=${URLEncoder.encode(LiferayContainer.DEFAULT_ADMIN_EMAIL, 'UTF-8')}&password=${URLEncoder.encode(pwd, 'UTF-8')}&rememberMe=true")
			)

			if (response.status() == 200) {
				loggedInPassword = pwd
				break
			}
		}

		if (loggedInPassword == null) {
			throw new IllegalStateException(
				"loginAsAdmin: no password in ${passwords} succeeded — check bootstrap state")
		}

		page.navigate("${liferay.baseUrl}/")
		page.waitForLoadState()

		if (page.title().contains('New Password')) {
			page.locator('#password1').fill(NEW_PASSWORD)
			page.locator('#password2').fill(NEW_PASSWORD)
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
			loggedInPassword = NEW_PASSWORD
		}

		activePassword = loggedInPassword ?: LiferayContainer.DEFAULT_ADMIN_PASSWORD

		if (page.locator('#reminderQueryAnswer').isVisible()) {
			page.locator('#reminderQueryAnswer').fill('test')
			page.waitForNavigation({ ->
				page.locator('[type=submit], button.btn-primary').first().click()
			})
		}

		return activePassword
	}

	protected static int httpGet(String url) {
		def connection = new URL(url).openConnection() as HttpURLConnection

		connection.requestMethod = 'GET'
		connection.connectTimeout = 30_000
		connection.readTimeout = 30_000

		return connection.responseCode
	}

	protected Map headlessGet(String path) {
		return _request('GET', path, 'application/json', null, null) { status, body ->
			if (status >= 400) {
				throw new IllegalStateException(
					"headlessGet ${path} returned HTTP ${status}: ${body}")
			}

			return new JsonSlurper().parseText(body) as Map
		}
	}

	protected Object jsonwsGet(String path) {
		return _request('GET', path, 'application/json', null, null) { status, body ->
			if (status >= 400) {
				throw new IllegalStateException(
					"jsonwsGet ${path} returned HTTP ${status}: ${body}")
			}

			if (!body?.trim() || body.trim() == 'null') {
				return null
			}

			return new JsonSlurper().parseText(body)
		}
	}

	protected Object jsonwsPost(String path, Map<String, Object> params) {
		String body = params.collect { k, v ->
			"${URLEncoder.encode(k as String, 'UTF-8')}=" +
				"${URLEncoder.encode(v == null ? '' : v.toString(), 'UTF-8')}"
		}.join('&')

		return _request(
				'POST', path, 'application/json',
				'application/x-www-form-urlencoded', body) { status, responseBody ->

			if (status >= 400) {
				throw new IllegalStateException(
					"jsonwsPost ${path} returned HTTP ${status}: ${responseBody}")
			}

			if (!responseBody?.trim() || responseBody.trim() == 'null') {
				return null
			}

			return new JsonSlurper().parseText(responseBody)
		}
	}

	protected Long getCompanyId() {
		if (cachedCompanyId == null) {
			// CompanyServiceUtil is in json.service.invalid.class.names and returns 403.
			// Use user/get-current-user (UserServiceUtil is not blacklisted) instead.
			def user = jsonwsGet('/api/jsonws/user/get-current-user') as Map
			cachedCompanyId = user.companyId as Long
		}

		return cachedCompanyId
	}

	protected Map headlessPost(String path, String jsonBody) {
		return _request(
				'POST', path, 'application/json', 'application/json',
				jsonBody) { status, body ->

			if (status >= 400) {
				throw new IllegalStateException(
					"headlessPost ${path} returned HTTP ${status}: ${body}")
			}

			return new JsonSlurper().parseText(body) as Map
		}
	}

	protected int headlessDelete(String path) {
		return _request('DELETE', path, null, null, null) { status, _body ->
			return status
		} as int
	}

	private Object _request(
			String method, String path, String acceptType, String contentType,
			String requestBody, Closure<Object> responseHandler) {

		def conn = new URL("${liferay.baseUrl}${path}").openConnection() as HttpURLConnection

		try {
			conn.requestMethod = method
			conn.connectTimeout = 10_000
			conn.readTimeout = 30_000
			conn.setRequestProperty('Authorization', basicAuthHeader())

			if (acceptType) {
				conn.setRequestProperty('Accept', acceptType)
			}

			if (contentType) {
				conn.setRequestProperty('Content-Type', contentType)
			}

			if (requestBody != null) {
				conn.doOutput = true
				conn.outputStream.withWriter('UTF-8') { writer ->
					writer.write(requestBody)
				}
			}

			int status = conn.responseCode
			String body = (status < 400)
				? (conn.inputStream?.text ?: '')
				: (conn.errorStream?.text ?: '')

			return responseHandler.call(status, body)
		}
		finally {
			conn.disconnect()
		}
	}

	protected String basicAuthHeader() {
		String credentials =
			"${LiferayContainer.DEFAULT_ADMIN_EMAIL}:${activePassword}"

		return "Basic ${credentials.bytes.encodeBase64().toString()}"
	}

	def cleanupSpec() {
		try {
			dumpJacocoCoverage(this.class.simpleName)
		}
		catch (Exception e) {
			log.warn('JaCoCo dump failed for {}: {}', this.class.simpleName, e.message, e)
		}
	}

	protected void dumpJacocoCoverage(String specName) {
		if (!liferay.isRunning()) {
			log.warn('Skipping JaCoCo dump for {} — container is not running', specName)
			return
		}

		if (!_isJacocoPortOpen()) {
			log.warn(
				'Skipping JaCoCo dump for {} — port {}:{} is not reachable. ' +
				'JaCoCo collection is out of scope for the DXP-native Docker flow; ' +
				'see docs/ADR/adr-0008-dxp-2026-migration.md',
				specName, liferay.host, liferay.jacocoPort)
			return
		}

		File outputFile = new File(System.getProperty('user.dir'), "build/jacoco/${specName}.exec")
		File jacocoDir = outputFile.parentFile
		if (!jacocoDir.mkdirs() && !jacocoDir.isDirectory()) {
			throw new IOException("Cannot create JaCoCo output directory: ${jacocoDir.absolutePath}")
		}

		Exception lastEx = null
		for (int i = 0; i < 3; i++) {
			try {
				new ExecDumpClient().dump(liferay.host, liferay.jacocoPort).save(outputFile, false)
				log.info('JaCoCo coverage dumped to {} ({} bytes)', outputFile.absolutePath, outputFile.length())
				return
			}
			catch (Exception e) {
				lastEx = e
				if (i < 2) {
					TimeUnit.SECONDS.sleep(2)
				}
			}
		}

		log.warn('JaCoCo dump failed for {} after 3 attempts: {}', specName, lastEx?.message)
	}

	private boolean _isJacocoPortOpen() {
		Socket socket = null
		try {
			socket = new Socket()
			socket.connect(new InetSocketAddress(liferay.host, liferay.jacocoPort), 1_000)
			return true
		}
		catch (IOException e) {
			return false
		}
		finally {
			try {
				socket?.close()
			}
			catch (IOException ignored) {
			}
		}
	}

}
