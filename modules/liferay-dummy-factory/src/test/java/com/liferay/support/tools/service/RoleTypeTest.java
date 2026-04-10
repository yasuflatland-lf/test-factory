package com.liferay.support.tools.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RoleTypeTest {

	@Test
	void fromStringUpperCase() {
		Assertions.assertEquals(
			RoleType.REGULAR, RoleType.fromString("REGULAR"));
	}

	@Test
	void fromStringLowerCase() {
		Assertions.assertEquals(RoleType.SITE, RoleType.fromString("site"));
	}

	@Test
	void fromStringInvalidThrowsException() {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> RoleType.fromString("invalid"));
	}

}
