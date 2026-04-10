package com.liferay.support.tools.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SiteMembershipTypeTest {

	@Test
	void fromStringUpperCase() {
		Assertions.assertEquals(
			SiteMembershipType.OPEN,
			SiteMembershipType.fromString("OPEN"));
	}

	@Test
	void fromStringLowerCase() {
		Assertions.assertEquals(
			SiteMembershipType.OPEN,
			SiteMembershipType.fromString("open"));
	}

	@Test
	void fromStringRestricted() {
		Assertions.assertEquals(
			SiteMembershipType.RESTRICTED,
			SiteMembershipType.fromString("restricted"));
	}

	@Test
	void fromStringUnknownThrowsException() {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> SiteMembershipType.fromString("unknown"));
	}

}
