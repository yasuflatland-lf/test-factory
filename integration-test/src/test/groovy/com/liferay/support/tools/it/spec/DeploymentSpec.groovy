package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.GogoShellClient

import spock.lang.Stepwise

@Stepwise
class DeploymentSpec extends BaseLiferaySpec {

	def 'Liferay container starts and is accessible'() {
		expect:
		liferay.running

		when:
		def responseCode = httpGet("${liferay.baseUrl}/c/portal/login")

		then:
		responseCode == 200
	}

	def 'Liferay Dummy Factory JAR deploys and bundle becomes ACTIVE'() {
		when:
		ensureDeployed()

		and:
		String output = ''

		new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
			output = gogo.execute('lb')
		}
		def matchingLine = output.readLines().find { it.contains('Liferay Dummy Factory') }

		then:
		matchingLine != null
		matchingLine.contains('Active')
	}

	def 'Portlet web resources are accessible after deployment'() {
		given:
		ensureDeployed()

		when:
		def responseCode = httpGet(
			"${liferay.baseUrl}/o/liferay-dummy-factory/__liferay__/index.js"
		)

		then:
		responseCode == 200
	}

	private static int httpGet(String url) {
		def connection = new URL(url).openConnection() as HttpURLConnection

		connection.requestMethod = 'GET'
		connection.connectTimeout = 10_000
		connection.readTimeout = 10_000

		return connection.responseCode
	}

}
