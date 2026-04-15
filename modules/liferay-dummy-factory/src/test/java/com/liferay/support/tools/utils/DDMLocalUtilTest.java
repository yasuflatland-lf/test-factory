package com.liferay.support.tools.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.dynamic.data.mapping.storage.Field;
import com.liferay.dynamic.data.mapping.storage.Fields;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DDMLocalUtil#toFields}.
 *
 * <p>
 * No Language stub is needed because {@link DDMLocalUtil} calls
 * {@code LocaleUtil.fromLanguageId(id, false)}, which skips locale validation
 * and performs pure string parsing — no portal services required.
 * </p>
 */
class DDMLocalUtilTest {

	private final DDMLocalUtil _util = new DDMLocalUtil();

	@Test
	void multipleFields_trailingCommaStripped() {
		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		fieldsMap.put("title", "Hello");
		fieldsMap.put("body", "World");
		fieldsMap.put("summary", "Short");

		Fields fields = _util.toFields(
			0L, fieldsMap, new String[] {"en_US"}, Locale.US);

		Field displayField = fields.get("_fieldsDisplay");

		assertNotNull(displayField, "_fieldsDisplay field must be present");

		String displayValue = (String)displayField.getValue(Locale.US, 0);

		assertNotNull(displayValue, "_fieldsDisplay value must not be null");
		assertFalse(
			displayValue.endsWith(","),
			"_fieldsDisplay must not end with a comma, was: " + displayValue);
		assertTrue(
			displayValue.contains("title_INSTANCE_"),
			"display value must contain 'title_INSTANCE_'");
		assertTrue(
			displayValue.contains("body_INSTANCE_"),
			"display value must contain 'body_INSTANCE_'");
		assertTrue(
			displayValue.contains("summary_INSTANCE_"),
			"display value must contain 'summary_INSTANCE_'");
	}

	@Test
	void singleField_noTrailingComma() {
		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		fieldsMap.put("content", "Only field");

		Fields fields = _util.toFields(
			0L, fieldsMap, new String[] {"en_US"}, Locale.US);

		Field displayField = fields.get("_fieldsDisplay");

		assertNotNull(displayField, "_fieldsDisplay field must be present");

		String displayValue = (String)displayField.getValue(Locale.US, 0);

		assertNotNull(displayValue, "_fieldsDisplay value must not be null");
		assertFalse(
			displayValue.endsWith(","),
			"single-field _fieldsDisplay must not end with a comma");
		assertTrue(
			displayValue.contains("content_INSTANCE_"),
			"display value must contain 'content_INSTANCE_'");
	}

	@Test
	void prePopulatedFieldsDisplay_generationSkipped() {
		String presetValue = "myField_INSTANCE_abcdefgh";

		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		fieldsMap.put("myField", "someValue");
		fieldsMap.put("_fieldsDisplay", presetValue);

		Fields fields = _util.toFields(
			0L, fieldsMap, new String[] {"en_US"}, Locale.US);

		Field displayField = fields.get("_fieldsDisplay");

		assertNotNull(displayField, "_fieldsDisplay field must be present");

		String displayValue = (String)displayField.getValue(Locale.US, 0);

		assertTrue(
			presetValue.equals(displayValue),
			"pre-populated _fieldsDisplay value must be used unchanged, " +
				"was: " + displayValue);
	}

	@Test
	void emptyMap_noException() {
		Map<String, Serializable> fieldsMap = new LinkedHashMap<>();

		assertDoesNotThrow(
			() -> _util.toFields(
				0L, fieldsMap, new String[] {"en_US"}, Locale.US),
			"toFields with an empty map must not throw");
	}

}
