package com.liferay.test.factory.it.container

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.DockerImageName

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class LiferayContainer extends GenericContainer<LiferayContainer> {

	static final int HTTP_PORT = 8080
	static final int GOGO_PORT = 11311
	static final String DEPLOY_DIR = '/opt/liferay/deploy/'
	static final String DEFAULT_ADMIN_EMAIL = 'test@liferay.com'
	static final String DEFAULT_ADMIN_PASSWORD = 'test'

	private static LiferayContainer INSTANCE

	LiferayContainer() {
		this('liferay/portal:7.4.3.120-ga120')
	}

	LiferayContainer(String imageName) {
		super(DockerImageName.parse(imageName))

		withExposedPorts(HTTP_PORT, GOGO_PORT)
		waitingFor(
			new LogMessageWaitStrategy()
				.withRegEx('.*org\\.apache\\.catalina\\.startup\\.Catalina\\.start Server startup in.*')
				.withStartupTimeout(Duration.ofMinutes(8))
		)
		withEnv([
			'LIFERAY_SETUP_WIZARD_ENABLED'       : 'false',
			'LIFERAY_TERMS_OF_USE_REQUIRED'       : 'false',
			'LIFERAY_USERS_REMINDER_QUERY_ENABLED': 'false',
		])
		withReuse(true)
	}

	static synchronized LiferayContainer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LiferayContainer()
			INSTANCE.start()
		}
		return INSTANCE
	}

	void deployJar(Path jarPath) {
		byte[] jarBytes = Files.readAllBytes(jarPath)
		String targetPath = DEPLOY_DIR + jarPath.fileName.toString()
		copyFileToContainer(Transferable.of(jarBytes), targetPath)
	}

	int getHttpPort() {
		return getMappedPort(HTTP_PORT)
	}

	int getGogoPort() {
		return getMappedPort(GOGO_PORT)
	}

	String getBaseUrl() {
		return "http://${host}:${httpPort}"
	}

}
