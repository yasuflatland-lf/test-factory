package com.liferay.support.tools.it.container

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
	static final int JACOCO_PORT = 6300
	static final String DEPLOY_DIR = '/opt/liferay/deploy/'
	static final String DEFAULT_ADMIN_EMAIL = 'test@liferay.com'
	static final String DEFAULT_ADMIN_PASSWORD = 'test'

	private static LiferayContainer INSTANCE

	LiferayContainer() {
		this(System.getProperty('liferay.docker.image', 'liferay/portal:7.4.3.132-ga132'))
	}

	private static final String PORTAL_EXT_PROPERTIES = [
		'setup.wizard.enabled=false',
		'terms.of.use.required=false',
		'users.reminder.query.enabled=false',
		'passwords.default.policy.change.required=false',
		'auth.verifier.BasicAuthHeaderAuthVerifier.urls.includes=/api/*',
	].join('\n') + '\n'

	LiferayContainer(String imageName) {
		super(DockerImageName.parse(imageName))

		withExposedPorts(HTTP_PORT, GOGO_PORT, JACOCO_PORT)
		waitingFor(
			new LogMessageWaitStrategy()
				.withRegEx('.*org\\.apache\\.catalina\\.startup\\.Catalina\\.start Server startup in.*')
				.withStartupTimeout(Duration.ofMinutes(8))
		)
		withEnv([
			'LIFERAY_SETUP_WIZARD_ENABLED'                       : 'false',
			'LIFERAY_TERMS_OF_USE_REQUIRED'                      : 'false',
			'LIFERAY_USERS_REMINDER_QUERY_ENABLED'               : 'false',
			'LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED'   : 'false',
			'LIFERAY_JVM_OPTS'                                    : "-javaagent:/tmp/jacocoagent.jar=output=tcpserver,port=${JACOCO_PORT},address=*".toString(),
		])
		withCopyToContainer(
			Transferable.of(PORTAL_EXT_PROPERTIES.bytes),
			'/opt/liferay/tomcat/webapps/ROOT/WEB-INF/classes/portal-ext.properties'
		)
		withReuse(false)
	}

	static synchronized LiferayContainer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LiferayContainer()
			INSTANCE.copyJacocoAgentToContainer()
			INSTANCE.start()
		}
		return INSTANCE
	}

	void copyJacocoAgentToContainer() {
		URL agentUrl = _findJacocoAgentUrl()
		if (!agentUrl) {
			throw new IllegalStateException(
				'jacocoagent.jar not found on test classpath. ' +
				'Ensure org.jacoco.agent:0.8.14:runtime is declared as a testImplementation dependency.'
			)
		}
		try {
			withCopyToContainer(Transferable.of(agentUrl.bytes), '/tmp/jacocoagent.jar')
		}
		catch (IOException e) {
			throw new IllegalStateException(
				"Failed to read jacocoagent.jar from ${agentUrl}: ${e.message}", e)
		}
	}

	private static URL _findJacocoAgentUrl() {
		String cp = System.getProperty('java.class.path', '')
		String found = cp.split(File.pathSeparator).find { _isJacocoAgentJar(it) }
		if (found) {
			return new File(found).toURI().toURL()
		}
		ClassLoader cl = Thread.currentThread().contextClassLoader
		while (cl != null) {
			if (cl instanceof URLClassLoader) {
				URL url = (cl as URLClassLoader).URLs.find { _isJacocoAgentJar(it.toExternalForm()) }
				if (url) {
					return url
				}
			}
			cl = cl.parent
		}
		return null
	}

	private static boolean _isJacocoAgentJar(String path) {
		return path?.contains('jacocoagent') ||
			(path?.contains('org.jacoco.agent') && path?.contains('runtime'))
	}

	void deployJar(Path jarPath) {
		byte[] jarBytes = Files.readAllBytes(jarPath)
		String fileName = jarPath.fileName.toString()
		String tmpPath = '/tmp/' + fileName
		String targetPath = DEPLOY_DIR + fileName
		copyFileToContainer(Transferable.of(jarBytes), tmpPath)
		execInContainer('bash', '-c', "cp ${tmpPath} ${targetPath} && chown liferay:liferay ${targetPath} && rm ${tmpPath}")
	}

	int getHttpPort() {
		return getMappedPort(HTTP_PORT)
	}

	int getGogoPort() {
		return getMappedPort(GOGO_PORT)
	}

	int getJacocoPort() {
		return getMappedPort(JACOCO_PORT)
	}

	String getBaseUrl() {
		return "http://${host}:${httpPort}"
	}

}
