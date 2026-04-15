package com.liferay.support.tools.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.dynamic.data.mapping.storage.Field;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.resource.bundle.ResourceBundleLoader;

import jakarta.portlet.PortletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DDMLocalUtil#toFields}.
 *
 * <p>
 * {@link LanguageUtil} requires a portal context because
 * {@link com.liferay.portal.kernel.util.LocaleUtil#fromLanguageId} validates
 * locale availability via {@link Language#isAvailableLocale(Locale)}. A
 * minimal stub is injected once before all tests so that validation succeeds
 * without a running Liferay instance.
 * </p>
 */
class DDMLocalUtilTest {

	@BeforeAll
	static void injectLanguageStub() throws Exception {
		java.lang.reflect.Field languageField =
			LanguageUtil.class.getDeclaredField("_language");

		languageField.setAccessible(true);
		languageField.set(null, new _StubLanguage());
	}

	// -------------------------------------------------------------------------
	// Test cases
	// -------------------------------------------------------------------------

	@Test
	void multipleFields_trailingCommaStripped() {
		DDMLocalUtil util = new DDMLocalUtil();

		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		fieldsMap.put("title", "Hello");
		fieldsMap.put("body", "World");
		fieldsMap.put("summary", "Short");

		Fields fields = util.toFields(
			0L, fieldsMap, new String[] {"en_US"}, Locale.ENGLISH);

		Field displayField =
			fields.get("_fieldsDisplay");

		assertNotNull(displayField, "_fieldsDisplay field must be present");

		String displayValue =
			(String)displayField.getValue(Locale.US, 0);

		assertNotNull(displayValue, "_fieldsDisplay value must not be null");
		assertFalse(
			displayValue.endsWith(","),
			"_fieldsDisplay must not end with a comma, was: " + displayValue);

		assertTrue(
			displayValue.contains("title"),
			"display value must contain 'title'");
		assertTrue(
			displayValue.contains("body"),
			"display value must contain 'body'");
		assertTrue(
			displayValue.contains("summary"),
			"display value must contain 'summary'");
		assertTrue(
			displayValue.contains("_INSTANCE_"),
			"display value must contain '_INSTANCE_' separator");
	}

	@Test
	void singleField_noTrailingComma() {
		DDMLocalUtil util = new DDMLocalUtil();

		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		fieldsMap.put("content", "Only field");

		Fields fields = util.toFields(
			0L, fieldsMap, new String[] {"en_US"}, Locale.ENGLISH);

		Field displayField =
			fields.get("_fieldsDisplay");

		assertNotNull(displayField, "_fieldsDisplay field must be present");

		String displayValue = (String)displayField.getValue(Locale.US, 0);

		assertNotNull(displayValue, "_fieldsDisplay value must not be null");
		assertFalse(
			displayValue.endsWith(","),
			"single-field _fieldsDisplay must not end with a comma");
		assertTrue(
			displayValue.contains("content"),
			"display value must contain 'content'");
	}

	@Test
	void prePopulatedFieldsDisplay_generationSkipped() {
		DDMLocalUtil util = new DDMLocalUtil();

		String presetValue = "myField_INSTANCE_abcdefgh";

		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		fieldsMap.put("myField", "someValue");
		fieldsMap.put("_fieldsDisplay", presetValue);

		Fields fields = util.toFields(
			0L, fieldsMap, new String[] {"en_US"}, Locale.ENGLISH);

		Field displayField =
			fields.get("_fieldsDisplay");

		assertNotNull(displayField, "_fieldsDisplay field must be present");

		String displayValue = (String)displayField.getValue(Locale.US, 0);

		// The pre-populated entry was placed as a regular field; after
		// toFields() runs, it finds a _fieldsDisplay field already present
		// and skips generation.  The stored value must equal the preset.
		assertTrue(
			presetValue.equals(displayValue),
			"pre-populated _fieldsDisplay value must be used unchanged, " +
				"was: " + displayValue);
	}

	@Test
	void emptyMap_noException() {
		DDMLocalUtil util = new DDMLocalUtil();

		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		assertDoesNotThrow(
			() -> util.toFields(
				0L, fieldsMap, new String[] {"en_US"}, Locale.ENGLISH),
			"toFields with an empty map must not throw");
	}

	// -------------------------------------------------------------------------
	// Minimal Language stub — only isAvailableLocale(Locale) is used by
	// LocaleUtil.fromLanguageId when validate=true.  Every other method throws
	// UnsupportedOperationException so accidental usage is caught immediately.
	// -------------------------------------------------------------------------

	private static final class _StubLanguage implements Language {

		@Override
		public boolean isAvailableLocale(Locale locale) {
			return true;
		}

		@Override
		public boolean isAvailableLocale(long groupId, Locale locale) {
			return true;
		}

		@Override
		public boolean isAvailableLocale(long groupId, String languageId) {
			return true;
		}

		@Override
		public boolean isAvailableLocale(String languageId) {
			return true;
		}

		@Override
		public boolean isAvailableLanguageCode(String languageCode) {
			return true;
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern,
			com.liferay.portal.kernel.language.LanguageWrapper argument) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern,
			com.liferay.portal.kernel.language.LanguageWrapper argument,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern,
			com.liferay.portal.kernel.language.LanguageWrapper[] arguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern,
			com.liferay.portal.kernel.language.LanguageWrapper[] arguments,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern, Object argument) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern, Object argument,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern, Object[] arguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			HttpServletRequest request, String pattern, Object[] arguments,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			Locale locale, String pattern, List<Object> arguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			Locale locale, String pattern, Object argument) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			Locale locale, String pattern, Object argument,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			Locale locale, String pattern, Object[] arguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			Locale locale, String pattern, Object[] arguments,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			ResourceBundle resourceBundle, String pattern, Object argument) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			ResourceBundle resourceBundle, String pattern, Object argument,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			ResourceBundle resourceBundle, String pattern, Object[] arguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String format(
			ResourceBundle resourceBundle, String pattern, Object[] arguments,
			boolean translateArguments) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String formatStorageSize(double size, Locale locale) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(
			HttpServletRequest request, ResourceBundle resourceBundle,
			String key) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String get(
			HttpServletRequest request, ResourceBundle resourceBundle,
			String key, String defaultValue) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String get(HttpServletRequest request, String key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(
			HttpServletRequest request, String key, String defaultValue) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String get(Locale locale, String key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(Locale locale, String key, String defaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(ResourceBundle resourceBundle, String key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(
			ResourceBundle resourceBundle, String key, String defaultValue) {

			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Locale> getAvailableLocaleMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Locale> getAvailableLocales() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Locale> getAvailableLocales(long groupId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getBCP47LangTag(Locale locale) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getBCP47LanguageId(HttpServletRequest request) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getBCP47LanguageId(Locale locale) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getBCP47LanguageId(PortletRequest request) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Locale> getCompanyAvailableLocales(long companyId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLanguageId(HttpServletRequest request) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLanguageId(Locale locale) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLanguageId(PortletRequest request) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Locale getLocale(long groupId, String languageCode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Locale getLocale(String languageCode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ResourceBundleLoader getResourceBundleLoader() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Locale> getSupportedLocales() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getTimeDescription(
			HttpServletRequest request, long milliseconds) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String getTimeDescription(
			HttpServletRequest request, long milliseconds, boolean approximate) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String getTimeDescription(
			HttpServletRequest request, Long milliseconds) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String getTimeDescription(Locale locale, long milliseconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getTimeDescription(
			Locale locale, long milliseconds, boolean approximate) {

			throw new UnsupportedOperationException();
		}

		@Override
		public String getTimeDescription(Locale locale, Long milliseconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void init() {
		}

		@Override
		public boolean isBetaLocale(Locale locale) {
			return false;
		}

		@Override
		public boolean isDuplicateLanguageCode(String languageCode) {
			return false;
		}

		@Override
		public boolean isInheritLocales(long groupId)
			throws com.liferay.portal.kernel.exception.PortalException {

			return false;
		}

		@Override
		public boolean isSameLanguage(Locale locale1, Locale locale2) {
			return locale1.getLanguage().equals(locale2.getLanguage());
		}

		@Override
		public String process(
			Supplier<ResourceBundle> resourceBundleSupplier, Locale locale,
			String content) {

			throw new UnsupportedOperationException();
		}

		@Override
		public void resetAvailableGroupLocales(long groupId) {
		}

		@Override
		public void resetAvailableLocales(long companyId) {
		}

		@Override
		public void updateCookie(
			HttpServletRequest request, HttpServletResponse response,
			Locale locale) {
		}

	}

}
