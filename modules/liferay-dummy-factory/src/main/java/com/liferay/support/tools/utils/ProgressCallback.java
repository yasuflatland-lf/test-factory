package com.liferay.support.tools.utils;

/**
 * Callback for reporting batch-creation progress from Creators to
 * ResourceCommands without coupling Creators to the portlet API.
 */
@FunctionalInterface
public interface ProgressCallback {

	void onProgress(int current, int total);

	static ProgressCallback noop() {
		return (c, t) -> {};
	}

}
