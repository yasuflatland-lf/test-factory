package com.liferay.support.tools.service;

/**
 * Typed value object that collapses the 18-argument {@code UserCreator.create}
 * signature into a single per-batch specification.  Nullable/empty inputs are
 * normalised to safe defaults in the compact constructor so callers never need
 * defensive blocks.
 */
public record UserBatchSpec(
	BatchSpec batch,
	String emailDomain,
	String password,
	boolean male,
	String jobTitle,
	long[] organizationIds,
	long[] roleIds,
	long[] userGroupIds,
	long[] siteRoleIds,
	long[] orgRoleIds,
	boolean fakerEnable,
	String locale,
	boolean generatePersonalSiteLayouts,
	long publicLayoutSetPrototypeId,
	long privateLayoutSetPrototypeId,
	long[] groupIds) {

	public UserBatchSpec {
		emailDomain = _nullOrEmptyToDefault(emailDomain, "liferay.com");
		password = _nullOrEmptyToDefault(password, "test");
		locale = _nullOrEmptyToDefault(locale, "en_US");
		jobTitle = (jobTitle == null) ? "" : jobTitle;
		organizationIds = _nullToEmpty(organizationIds);
		roleIds = _nullToEmpty(roleIds);
		userGroupIds = _nullToEmpty(userGroupIds);
		siteRoleIds = _nullToEmpty(siteRoleIds);
		orgRoleIds = _nullToEmpty(orgRoleIds);
		groupIds = _nullToEmpty(groupIds);
	}

	private static long[] _nullToEmpty(long[] array) {
		return (array == null) ? new long[0] : array;
	}

	private static String _nullOrEmptyToDefault(
		String value, String defaultValue) {

		if ((value == null) || value.isEmpty()) {
			return defaultValue;
		}

		return value;
	}

}
