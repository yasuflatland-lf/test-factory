import {FieldDefinition, Validator} from '../types';

export function validateField(
	value: string,
	field: FieldDefinition
): string | null {
	if (field.required && !value.trim()) {
		return Liferay.Language.get('this-field-is-required');
	}

	if (field.validators) {
		for (const validator of field.validators) {
			const error = runValidator(value, validator);

			if (error) {
				return error;
			}
		}
	}

	return null;
}

function runValidator(value: string, validator: Validator): string | null {
	switch (validator.type) {
		case 'digits':
			if (value && !/^\d+$/.test(value)) {
				return Liferay.Language.get(validator.message);
			}
			break;
		case 'min':
			if (validator.value !== undefined && Number(value) < validator.value) {
				return Liferay.Language.get(validator.message);
			}
			break;
		case 'max':
			if (validator.value !== undefined && Number(value) > validator.value) {
				return Liferay.Language.get(validator.message);
			}
			break;
		case 'required':
			if (!value.trim()) {
				return Liferay.Language.get(validator.message);
			}
			break;
	}

	return null;
}

export function validateForm(
	values: Record<string, string>,
	fields: FieldDefinition[]
): Record<string, string> {
	const errors: Record<string, string> = {};

	for (const field of fields) {
		const error = validateField(values[field.name] || '', field);

		if (error) {
			errors[field.name] = error;
		}
	}

	return errors;
}
