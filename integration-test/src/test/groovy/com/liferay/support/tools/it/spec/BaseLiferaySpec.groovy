package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.container.LiferayContainer
import com.liferay.support.tools.it.util.GogoShellClient

import spock.lang.Shared
import spock.lang.Specification

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class BaseLiferaySpec extends Specification {

	private static final Logger log = LoggerFactory.getLogger(BaseLiferaySpec)

	@Shared
	static LiferayContainer liferay = LiferayContainer.getInstance()

	@Shared
	static boolean jarDeployed = false

	static Path getCalculatorJarPath() {
		Path workspaceRoot = Path.of(System.getProperty('user.dir')).parent
		Path jarDir = workspaceRoot.resolve(
			'modules/liferay-dummy-factory/build/libs'
		)
		File[] jars = jarDir.toFile().listFiles({ File f ->
			f.name.endsWith('.jar') && f.name.contains('liferay.dummy.factory')
		} as FileFilter)

		if (jars == null || jars.length == 0) {
			throw new IllegalStateException(
				"Calculator JAR not found in ${jarDir}. " +
				"Run './gradlew :modules:liferay-dummy-factory:jar' first."
			)
		}

		return jars[0].toPath()
	}

	static synchronized void ensureDeployed() {
		if (jarDeployed) {
			return
		}

		log.info('Deploying JAR: {}', getCalculatorJarPath())
		liferay.deployJar(getCalculatorJarPath())
		log.info('JAR copied to container. GoGo Shell at {}:{}', liferay.host, liferay.gogoPort)

		boolean active = false

		for (int i = 0; i < 60; i++) {
			try {
				new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
					String output = gogo.execute('lb')
					def allLines = output.readLines()
					int lineCount = allLines.size()
					// Log last 5 lines to see actual bundle names
					def tail = allLines.takeRight(5)
					log.info('GoGo Shell attempt {}: {} lines, last 5: {}', i + 1, lineCount, tail)
					// Search with relaxed matching
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

		jarDeployed = true
	}

}
