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

	private static LiferayContainer INSTANCE

	private static final Logger log = LoggerFactory.getLogger(LiferayContainer)

	final String host
	final int httpPort
	final int gogoPort
	final int jacocoPort
	final String containerName

	private LiferayContainer() {
		this.host = System.getProperty('liferay.host', 'localhost')
		this.httpPort = Integer.parseInt(
			System.getProperty('liferay.http.port', String.valueOf(HTTP_PORT)))
		this.gogoPort = Integer.parseInt(
			System.getProperty('liferay.gogo.port', String.valueOf(GOGO_PORT)))
		this.jacocoPort = Integer.parseInt(
			System.getProperty('liferay.jacoco.port', String.valueOf(JACOCO_PORT)))
		this.containerName = System.getProperty(
			'liferay.container.name', 'test-factory-liferay')
	}

	static synchronized LiferayContainer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LiferayContainer()
			log.info(
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
	 * Copy the given module JAR into the running DXP container's hot-deploy directory.
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

		boolean finished = process.waitFor(60, TimeUnit.SECONDS)
		if (!finished) {
			process.destroyForcibly()
			throw new IllegalStateException(
				"docker cp timed out after 60s for ${jarPath} -> ${target}: ${output}")
		}

		int exit = process.exitValue()
		if (exit != 0) {
			throw new IllegalStateException(
				"docker cp failed (exit ${exit}) for ${jarPath} -> ${target}: ${output}")
		}

		log.info('Deployed {} into {}', fileName, target)
	}

	/**
	 * Fast readiness probe used by specs. Checks that the container exposes the
	 * HTTP port and that Liferay responds. Returns false on any I/O error so callers
	 * can warn-and-skip (e.g. JaCoCo dump) instead of aborting tests.
	 */
	boolean isRunning() {
		try {
			HttpURLConnection conn =
				(HttpURLConnection) new URL("${baseUrl}/c/portal/login").openConnection()
			try {
				conn.connectTimeout = 3_000
				conn.readTimeout = 3_000
				conn.requestMethod = 'GET'
				int code = conn.responseCode
				return code > 0
			}
			finally {
				conn.disconnect()
			}
		}
		catch (Exception e) {
			return false
		}
	}

}
