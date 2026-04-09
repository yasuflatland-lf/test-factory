package com.liferay.test.factory.it.spec

import com.liferay.test.factory.it.util.GogoShellClient

import spock.lang.Stepwise

@Stepwise
class DeploymentSpec extends BaseLiferaySpec {

	def 'Liferay container starts and is accessible'() {
		expect:
		liferay.running

		when:
		def connection = new URL("${liferay.baseUrl}/c/portal/login")
			.openConnection() as HttpURLConnection
		connection.requestMethod = 'GET'
		connection.connectTimeout = 10_000
		connection.readTimeout = 10_000
		def responseCode = connection.responseCode

		then:
		responseCode == 200
	}

	def 'Calculator JAR deploys and bundle becomes ACTIVE'() {
		when:
		ensureDeployed()

		and:
		def gogo = new GogoShellClient(liferay.host, liferay.gogoPort)
		def output = gogo.execute('lb | grep test.factory')
		gogo.close()

		then:
		output.contains('Active') || output.contains('ACTIVE')
		output.contains('com.liferay.test.factory')
	}

	def 'JSONWS endpoint is registered after deployment'() {
		given:
		ensureDeployed()

		when:
		def url = "${liferay.baseUrl}/api/jsonws?contextName=TestFactory"
		def connection = new URL(url).openConnection() as HttpURLConnection
		connection.requestMethod = 'GET'
		connection.connectTimeout = 10_000
		connection.readTimeout = 10_000
		def responseCode = connection.responseCode

		then:
		responseCode == 200
	}

}
