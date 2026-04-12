package com.liferay.support.tools.utils;

/**
 * Callback for reporting batch-creation progress from Creators to
 * ResourceCommands without coupling Creators to the portlet API.
 */
@FunctionalInterface
public interface ProgressCallback {

	void onProgress(long current, long total);

	static final ProgressCallback NOOP = (c, t) -> {};

}
