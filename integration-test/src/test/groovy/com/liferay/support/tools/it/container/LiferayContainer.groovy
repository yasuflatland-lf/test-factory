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
		this(System.getProperty('liferay.docker.image', 'liferay/dxp:2026.q1.3-lts'))
	}

	LiferayContainer(String imageName) {
		super(DockerImageName.parse(imageName))

		withExposedPorts(HTTP_PORT, GOGO_PORT, JACOCO_PORT)
		waitingFor(
			new LogMessageWaitStrategy()
				.withRegEx('.*org\\.apache\\.catalina\\.startup\\.Catalina\\.start Server startup in.*')
				.withStartupTimeout(Duration.ofMinutes(8))
		)
		withEnv([
			'LIFERAY_SETUP_WIZARD_ENABLED'                                               : 'false',
			'LIFERAY_TERMS_OF_USE_REQUIRED'                                              : 'false',
			'LIFERAY_USERS_REMINDER_QUERY_ENABLED'                                       : 'false',
			'LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED'                           : 'false',
			'LIFERAY_JVM_OPTS'                                                            : "-javaagent:/tmp/jacocoagent.jar=output=tcpserver,port=${JACOCO_PORT},address=*".toString(),
			'LIFERAY_DISABLE_TRIAL_LICENSE'                                              : 'true',
			'LIFERAY_ENTERPRISE_PERIOD_PRODUCT_PERIOD_NOTIFICATION_PERIOD_ENABLED'       : 'false',
		])
		_copyPortalExtProperties()
		_copyPortalOnlineConfig()
		withReuse(false)
	}

	static synchronized LiferayContainer getInstance() {
		if (INSTANCE == null) {
			LiferayContainer container = new LiferayContainer()
			container.copyJacocoAgentToContainer()
			container.copyLicenseToContainer()
			container.start()
			INSTANCE = container
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

	void copyLicenseToContainer() {
		String licenseFilePath = System.getenv('LIFERAY_DXP_LICENSE_FILE')
		String licenseBase64 = System.getenv('LIFERAY_DXP_LICENSE_BASE64')

		byte[] licenseBytes

		if (licenseFilePath) {
			File f = new File(licenseFilePath)
			if (!f.exists()) {
				throw new IllegalStateException(
					"LIFERAY_DXP_LICENSE_FILE points to a non-existent file: ${licenseFilePath}")
			}
			if (!f.canRead()) {
				throw new IllegalStateException(
					"LIFERAY_DXP_LICENSE_FILE exists but is not readable: ${licenseFilePath} (check file permissions)")
			}
			try {
				licenseBytes = f.bytes
			}
			catch (IOException e) {
				throw new IllegalStateException(
					"Failed to read license file at ${licenseFilePath}: ${e.message}", e)
			}
		}
		else if (licenseBase64) {
			try {
				licenseBytes = Base64.decoder.decode(licenseBase64.trim())
			}
			catch (IllegalArgumentException e) {
				throw new IllegalStateException(
					"LIFERAY_DXP_LICENSE_BASE64 contains invalid base64 content: ${e.message}. " +
					'Ensure the value is standard base64 (not URL-safe or MIME-split) with no embedded newlines. ' +
					'Re-encode with: base64 -w0 activation-key.xml', e)
			}
		}
		else {
			throw new IllegalStateException(
				'DXP license not found. Set LIFERAY_DXP_LICENSE_FILE (path) ' +
				'or LIFERAY_DXP_LICENSE_BASE64 (base64-encoded content) before running integration tests.')
		}

		if (licenseBytes.length == 0) {
			throw new IllegalStateException(
				'DXP license resolved to an empty file. ' +
				'Check that LIFERAY_DXP_LICENSE_FILE is a non-empty file or that ' +
				'LIFERAY_DXP_LICENSE_BASE64 encodes actual license content.')
		}

		try {
			withCopyToContainer(Transferable.of(licenseBytes), '/opt/liferay/osgi/modules/activation-key.xml')
		}
		catch (Exception e) {
			throw new IllegalStateException(
				"Failed to copy DXP license into container: ${e.message}", e)
		}
	}

	private void _copyPortalExtProperties() {
		withCopyToContainer(
			Transferable.of(_mergeConfigFiles().getBytes('UTF-8')),
			'/mnt/liferay/files/portal-ext.properties'
		)
	}

	private void _copyPortalOnlineConfig() {
		// DXP 2026: portal-liferay-online-config.properties, baked into the Docker image
		// (portal-impl.jar / WEB-INF/classes/), sets json.servlet.hosts.allowed=N/A, which
		// blocks all JSONWS access. Place an empty file via /mnt/liferay/files/ so the Docker
		// entrypoint writes it into Liferay Home before startup, shadowing the baked-in value
		// through the portal.properties loading chain.
		withCopyToContainer(
			Transferable.of(''.bytes),
			'/mnt/liferay/files/portal-liferay-online-config.properties'
		)
	}

	private static String _mergeConfigFiles() {
		String projectRoot = System.getProperty('project.root.dir')
		if (!projectRoot) {
			throw new IllegalStateException(
				'System property "project.root.dir" is not set. ' +
				'Add systemProperty("project.root.dir", rootProject.projectDir.absolutePath) ' +
				'to the integrationTest task in integration-test/build.gradle.'
			)
		}

		// Concatenate raw file content rather than parsing through Properties, because
		// Properties.store() would escape special characters (quotes, backslashes) and
		// corrupt B"true" boolean values and double-quoted URL patterns in OSGi
		// configuration override keys. Liferay's loader applies last-wins on duplicate
		// keys, so docker entries naturally override common entries.
		StringBuilder sb = new StringBuilder()
		['common', 'docker'].each { String env ->
			File dir = new File(projectRoot, "configs/${env}")
			if (dir.isDirectory()) {
				dir.listFiles()
					?.findAll { it.name == 'portal-ext.properties' }
					?.each { File f ->
						try {
							sb.append(f.getText('UTF-8')).append('\n')
						}
						catch (IOException e) {
							throw new IllegalStateException(
								"Failed to read config file '${f.absolutePath}': ${e.message}", e)
						}
					}
			}
		}
		if (sb.length() == 0) {
			throw new IllegalStateException(
				'No portal-ext.properties files found under configs/common or configs/docker. ' +
				"Verify project root: '${projectRoot}'"
			)
		}
		return sb.toString()
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
