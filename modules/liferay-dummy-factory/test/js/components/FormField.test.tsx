import {render, screen} from '@testing-library/react';

import FormField from '../../../src/main/resources/META-INF/resources/js/components/FormField';
import {FieldDefinition} from '../../../src/main/resources/META-INF/resources/js/types';

describe('FormField i18n', () => {
	const noop = () => {
		// no-op
	};

	const textField: FieldDefinition = {
		label: 'name',
		name: 'name',
		required: true,
		type: 'text',
	};

	const selectField: FieldDefinition = {
		label: 'role',
		name: 'role',
		options: [
			{label: 'administrator', value: 'admin'},
			{label: 'user', value: 'user'},
		],
		required: true,
		type: 'select',
	};

	it('renders the required validation i18n message when error is set on a text field', () => {
		const requiredText = Liferay.Language.get('this-field-is-required');

		expect(requiredText).not.toBe('this-field-is-required');
		expect(requiredText.length).toBeGreaterThan(0);

		render(
			<FormField
				error={requiredText}
				field={textField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(requiredText)).not.toBeNull();
	});

	it('renders the required validation i18n message when error is set on a textarea field', () => {
		const requiredText = Liferay.Language.get('this-field-is-required');

		expect(requiredText).not.toBe('this-field-is-required');
		expect(requiredText.length).toBeGreaterThan(0);

		const textareaField: FieldDefinition = {
			label: 'description',
			name: 'description',
			required: true,
			type: 'textarea',
		};

		render(
			<FormField
				error={requiredText}
				field={textareaField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(requiredText)).not.toBeNull();
	});

	it('renders the select placeholder i18n message for select fields', () => {
		const selectText = Liferay.Language.get('select');

		expect(selectText).not.toBe('select');
		expect(selectText.length).toBeGreaterThan(0);

		render(
			<FormField
				field={selectField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(selectText)).not.toBeNull();
	});

	it('renders the required validation i18n message alongside the select placeholder when error is set', () => {
		const selectText = Liferay.Language.get('select');
		const requiredText = Liferay.Language.get('this-field-is-required');

		expect(selectText).not.toBe('select');
		expect(selectText.length).toBeGreaterThan(0);
		expect(requiredText).not.toBe('this-field-is-required');
		expect(requiredText.length).toBeGreaterThan(0);

		render(
			<FormField
				error={requiredText}
				field={selectField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(selectText)).not.toBeNull();
		expect(screen.queryByText(requiredText)).not.toBeNull();
	});
});
