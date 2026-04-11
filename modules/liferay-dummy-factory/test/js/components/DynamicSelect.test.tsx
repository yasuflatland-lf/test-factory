import {render, screen} from '@testing-library/react';

import DynamicSelect from '../../../src/main/resources/META-INF/resources/js/components/DynamicSelect';
import {FieldDefinition} from '../../../src/main/resources/META-INF/resources/js/types';

const mockUseApiData = jest.fn();

jest.mock(
	'../../../src/main/resources/META-INF/resources/js/hooks/useApiData',
	() => ({
		useApiData: (...args: unknown[]) => mockUseApiData(...args),
	})
);

const baseField: FieldDefinition = {
	dataSource: '/ldf/data/roles',
	label: 'role',
	name: 'roleId',
	required: false,
	type: 'select',
};

const dependentField: FieldDefinition = {
	dataSource: '/ldf/data/org-roles',
	dependsOn: {
		field: 'organizationId',
		paramName: 'organizationId',
	},
	label: 'organization-role',
	name: 'orgRoleId',
	required: false,
	type: 'select',
};

function idleApiData() {
	return {
		data: [],
		error: null,
		loading: false,
		reload: jest.fn(),
	};
}

describe('DynamicSelect i18n', () => {
	beforeEach(() => {
		mockUseApiData.mockReset();
	});

	it('renders the parent-selection guidance when dependsOn has no value', () => {
		mockUseApiData.mockReturnValue(idleApiData());

		render(
			<DynamicSelect
				dataResourceURL="/o/data"
				dependsOnValue=""
				field={dependentField}
				onChange={jest.fn()}
				value=""
			/>
		);

		expect(
			screen.getByText('Please select the parent field first')
		).not.toBeNull();
	});

	it('disables the select while showing the parent-selection guidance', () => {
		mockUseApiData.mockReturnValue(idleApiData());

		render(
			<DynamicSelect
				dataResourceURL="/o/data"
				dependsOnValue={undefined}
				field={dependentField}
				onChange={jest.fn()}
				testId="org-role-select"
				value=""
			/>
		);

		const select = screen.getByTestId(
			'org-role-select'
		) as HTMLSelectElement;

		expect(select.disabled).toBe(true);
	});

	it('renders the default Select placeholder option when data is loaded', () => {
		mockUseApiData.mockReturnValue(idleApiData());

		render(
			<DynamicSelect
				dataResourceURL="/o/data"
				field={baseField}
				onChange={jest.fn()}
				value=""
			/>
		);

		const option = screen.getByRole('option', {
			name: 'Select',
		}) as HTMLOptionElement;

		expect(option).not.toBeNull();
		expect(option.value).toBe('');
	});

	it('does not render the parent-selection guidance when dependsOnValue is provided', () => {
		mockUseApiData.mockReturnValue(idleApiData());

		render(
			<DynamicSelect
				dataResourceURL="/o/data"
				dependsOnValue="123"
				field={dependentField}
				onChange={jest.fn()}
				value=""
			/>
		);

		expect(
			screen.queryByText('Please select the parent field first')
		).toBeNull();
	});

	it('does not render a select while loading', () => {
		mockUseApiData.mockReturnValue({
			data: [],
			error: null,
			loading: true,
			reload: jest.fn(),
		});

		const {container} = render(
			<DynamicSelect
				dataResourceURL="/o/data"
				field={baseField}
				onChange={jest.fn()}
				value=""
			/>
		);

		expect(container.querySelector('select')).toBeNull();
		expect(container.querySelector('.loading-animation')).not.toBeNull();
	});
});
