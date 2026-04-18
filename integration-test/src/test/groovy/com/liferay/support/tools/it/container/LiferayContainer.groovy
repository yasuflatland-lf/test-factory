package com.liferay.support.tools.it.container

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Fixed-port client for the DXP container that is created and started by the
 * Liferay workspace plugin's {@code createDockerContainer} / {@code startDockerContainer}
 * Gradle tasks. This class never starts or stops a container — the Gradle task graph
 * owns that lifecycle. All specs share a single instance via {@link #getInstance()}.
 */
class LiferayContainer {

	static final int HTTP_PORT = 8080
	static final int GOGO_PORT = 11311
	static final int JACOCO_PORT = 6300
	static final String DEPLOY_DIR = '/opt/liferay/deploy/'
	static final String DEFAULT_ADMIN_EMAIL = 'test@liferay.com'
	static final String DEFAULT_ADMIN_PASSWORD = 'test'

	private static final Logger _log = LoggerFactory.getLogger(LiferayContainer)

	private static LiferayContainer INSTANCE

	final String host
	final int httpPort
	final int gogoPort
	final int jacocoPort
	final String containerName

	private LiferayContainer() {
		host = System.getProperty('liferay.host', 'localhost')
		httpPort = Integer.parseInt(
			System.getProperty('liferay.http.port', "${HTTP_PORT}"))
		gogoPort = Integer.parseInt(
			System.getProperty('liferay.gogo.port', "${GOGO_PORT}"))
		jacocoPort = Integer.parseInt(
			System.getProperty('liferay.jacoco.port', "${JACOCO_PORT}"))
		containerName = System.getProperty(
			'liferay.container.name', 'test-factory-liferay')
	}

	static synchronized LiferayContainer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LiferayContainer()
			_log.info(
				'LiferayContainer pinned to {}:{} (gogo={}, jacoco={}, container={})',
				INSTANCE.host, INSTANCE.httpPort, INSTANCE.gogoPort,
				INSTANCE.jacocoPort, INSTANCE.containerName)
		}
		return INSTANCE
	}

	String getBaseUrl() {
		return "http://${host}:${httpPort}"
	}

	/**
	 * Uses {@code docker cp}, which requires the container to be running and the current
	 * user to have permission to talk to the Docker daemon.
	 */
	void deployJar(Path jarPath) {
		String fileName = jarPath.fileName.toString()
		String target = "${containerName}:${DEPLOY_DIR}${fileName}"

		ProcessBuilder pb = new ProcessBuilder(
			'docker', 'cp', jarPath.toAbsolutePath().toString(), target)
		pb.redirectErrorStream(true)

		Process process = pb.start()
		String output = process.inputStream.text

		if (!process.waitFor(60, TimeUnit.SECONDS)) {
			process.destroyForcibly()
			throw new IllegalStateException(
				"docker cp timed out after 60s for ${jarPath} -> ${target}: ${output}")
		}

		int exit = process.exitValue()
		if (exit != 0) {
			throw new IllegalStateException(
				"docker cp failed (exit ${exit}) for ${jarPath} -> ${target}: ${output}")
		}

		_log.info('Deployed {} into {}', fileName, target)
	}

	/**
	 * Returns false on any I/O error so callers can warn-and-skip (e.g. JaCoCo dump)
	 * instead of aborting tests.
	 */
	boolean isRunning() {
		try {
			HttpURLConnection conn =
				(HttpURLConnection) new URL("${baseUrl}/c/portal/login").openConnection()
			try {
				conn.connectTimeout = 3_000
				conn.readTimeout = 3_000
				return conn.responseCode > 0
			}
			finally {
				conn.disconnect()
			}
		}
		catch (IOException ignored) {
			return false
		}
	}

}
