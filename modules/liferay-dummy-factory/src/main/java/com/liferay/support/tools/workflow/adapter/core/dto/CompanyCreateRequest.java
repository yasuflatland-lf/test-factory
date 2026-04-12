package com.liferay.support.tools.workflow.adapter.core.dto;

import java.util.Objects;

public record CompanyCreateRequest(
	boolean active, int count, int maxUsers, String mx, String virtualHostname,
	String webId) {

	public CompanyCreateRequest {
		if (count <= 0) {
			throw new IllegalArgumentException(
				"count must be greater than 0");
		}

		mx = _requireText(mx, "mx");
		virtualHostname = _requireText(virtualHostname, "virtualHostname");
		webId = _requireText(webId, "webId");
	}

	private static String _requireText(String value, String fieldName) {
		Objects.requireNonNull(value, fieldName + " is required");

		if (value.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be empty");
		}

		return value;
	}

}
