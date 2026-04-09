package com.liferay.test.factory.it.util

import org.apache.commons.net.telnet.TelnetClient

class GogoShellClient implements Closeable {

	private final TelnetClient telnet
	private final InputStream inputStream
	private final OutputStream outputStream

	GogoShellClient(String host, int port) {
		telnet = new TelnetClient()
		telnet.setConnectTimeout(5000)
		telnet.connect(host, port)
		inputStream = telnet.inputStream
		outputStream = telnet.outputStream
		readUntilPrompt()
	}

	String execute(String command) {
		outputStream.write((command + '\n').bytes)
		outputStream.flush()
		return readUntilPrompt()
	}

	private String readUntilPrompt() {
		StringBuilder sb = new StringBuilder()
		long timeout = System.currentTimeMillis() + 10_000

		while (System.currentTimeMillis() < timeout) {
			if (inputStream.available() > 0) {
				int ch = inputStream.read()

				if (ch == -1) {
					break
				}

				sb.append((char) ch)

				if (sb.toString().endsWith('g! ')) {
					break
				}
			}
			else {
				Thread.sleep(100)
			}
		}

		return sb.toString()
	}

	@Override
	void close() {
		try {
			telnet.disconnect()
		}
		catch (ignored) {
		}
	}

}
