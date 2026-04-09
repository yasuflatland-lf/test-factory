package com.liferay.test.factory.it.spec

import com.liferay.test.factory.it.container.LiferayContainer
import com.liferay.test.factory.it.util.GogoShellClient

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class BaseLiferaySpec extends Specification {

	@Shared
	static LiferayContainer liferay = LiferayContainer.getInstance()

	@Shared
	static boolean jarDeployed = false

	static Path getCalculatorJarPath() {
		Path workspaceRoot = Path.of(System.getProperty('user.dir')).parent
		Path jarDir = workspaceRoot.resolve(
			'modules/test-factory-calculator/build/libs'
		)
		File[] jars = jarDir.toFile().listFiles({ File f ->
			f.name.endsWith('.jar') && f.name.contains('test.factory')
		} as FileFilter)

		if (jars == null || jars.length == 0) {
			throw new IllegalStateException(
				"Calculator JAR not found in ${jarDir}. " +
				"Run './gradlew :modules:test-factory-calculator:jar' first."
			)
		}

		return jars[0].toPath()
	}

	static synchronized void ensureDeployed() {
		if (jarDeployed) {
			return
		}

		liferay.deployJar(getCalculatorJarPath())

		boolean active = false

		for (int i = 0; i < 60; i++) {
			try {
				new GogoShellClient(liferay.host, liferay.gogoPort).withCloseable { gogo ->
					String output = gogo.execute('lb | grep test.factory')

					if (output.contains('Active') || output.contains('ACTIVE')) {
						active = true
					}
				}

				if (active) {
					break
				}
			}
			catch (Exception ignored) {
			}

			TimeUnit.SECONDS.sleep(5)
		}

		if (!active) {
			throw new IllegalStateException(
				'Bundle com.liferay.test.factory did not reach ACTIVE ' +
				'state within timeout'
			)
		}

		jarDeployed = true
	}

}
