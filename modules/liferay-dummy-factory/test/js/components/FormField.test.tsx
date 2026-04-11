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
		render(
			<FormField
				error={Liferay.Language.get('this-field-is-required')}
				field={textField}
				onChange={noop}
				value=""
			/>
		);

		expect(
			screen.queryByText(
				Liferay.Language.get('this-field-is-required')
			)
		).not.toBeNull();
	});

	it('renders the required validation i18n message when error is set on a textarea field', () => {
		const textareaField: FieldDefinition = {
			label: 'description',
			name: 'description',
			required: true,
			type: 'textarea',
		};

		render(
			<FormField
				error={Liferay.Language.get('this-field-is-required')}
				field={textareaField}
				onChange={noop}
				value=""
			/>
		);

		expect(
			screen.queryByText(
				Liferay.Language.get('this-field-is-required')
			)
		).not.toBeNull();
	});

	it('renders the select placeholder i18n message for select fields', () => {
		render(
			<FormField
				field={selectField}
				onChange={noop}
				value=""
			/>
		);

		expect(
			screen.queryByText(Liferay.Language.get('select'))
		).not.toBeNull();
	});

	it('renders the required validation i18n message alongside the select placeholder when error is set', () => {
		render(
			<FormField
				error={Liferay.Language.get('this-field-is-required')}
				field={selectField}
				onChange={noop}
				value=""
			/>
		);

		expect(
			screen.queryByText(Liferay.Language.get('select'))
		).not.toBeNull();
		expect(
			screen.queryByText(
				Liferay.Language.get('this-field-is-required')
			)
		).not.toBeNull();
	});
});
