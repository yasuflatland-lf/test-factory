package com.liferay.support.tools.service;

import org.junit.Assert;
import org.junit.Test;

public class BatchNamingTest {

	@Test
	public void testSingleItemUsesBaseNameDirectly() {
		Assert.assertEquals("Site", BatchNaming.resolve("Site", 1, 0));
	}

	@Test
	public void testMultipleItemsFirstIndex() {
		Assert.assertEquals("Site1", BatchNaming.resolve("Site", 3, 0));
	}

	@Test
	public void testMultipleItemsLastIndex() {
		Assert.assertEquals("Site3", BatchNaming.resolve("Site", 3, 2));
	}

	@Test
	public void testDifferentBaseNameLastItem() {
		Assert.assertEquals("Role5", BatchNaming.resolve("Role", 5, 4));
	}

	@Test
	public void testSeparatorVariant() {
		Assert.assertEquals("Org 1", BatchNaming.resolve("Org", 3, 0, " "));
	}

}
