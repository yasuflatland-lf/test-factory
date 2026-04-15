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

import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class BaseLiferaySpec extends Specification {

	protected static final String NEW_PASSWORD = 'Test12345'
	protected static final String PORTLET_ID = 'com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet'

	private static final Logger log = LoggerFactory.getLogger(BaseLiferaySpec)

	@Shared
	static LiferayContainer liferay = LiferayContainer.getInstance()

	@Shared
	static boolean bundleVerified = false

	@Shared
	static String activePassword = NEW_PASSWORD

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

	static synchronized void ensureBundleActive() {
		if (bundleVerified) {
			return
		}

		log.info('Deploying JAR: {}', getModuleJarPath())
		liferay.deployJar(getModuleJarPath())
		log.info('JAR copied to container. GoGo Shell at {}:{}', liferay.host, liferay.gogoPort)

		boolean active = false

		for (int i = 0; i < 60; i++) {
			try {
				new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
					String output = gogo.execute('lb')
					def allLines = output.readLines()
					int lineCount = allLines.size()
					def tail = allLines.takeRight(5)
					log.info('GoGo Shell attempt {}: {} lines, last 5: {}', i + 1, lineCount, tail)
					def lines = allLines.findAll { it.toLowerCase().contains('liferay') && it.toLowerCase().contains('dummy') && it.toLowerCase().contains('factory') }
					log.info('Matches: {}', lines ?: '(no match)')

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

			TimeUnit.SECONDS.sleep(5)
		}

		if (!active) {
			throw new IllegalStateException(
				'Bundle liferay.dummy.factory did not reach ACTIVE ' +
				'state within timeout'
			)
		}

		bundleVerified = true
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
			def company = jsonwsGet(
				'/api/jsonws/company/get-company-by-virtual-host' +
				'/virtual-host/localhost') as Map
			cachedCompanyId = company.companyId as Long
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

		File outputFile = new File(System.getProperty('user.dir'), "build/jacoco/${specName}.exec")
		File jacocoDir = outputFile.parentFile
		if (!jacocoDir.mkdirs() && !jacocoDir.isDirectory()) {
			throw new IOException("Cannot create JaCoCo output directory: ${jacocoDir.absolutePath}")
		}

		new ExecDumpClient().dump(liferay.host, liferay.jacocoPort).save(outputFile, false)

		log.info('JaCoCo coverage dumped to {}', outputFile.absolutePath)
	}

}
