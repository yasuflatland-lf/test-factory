import {render, screen} from '@testing-library/react';

import EntityForm from '../../../src/main/resources/META-INF/resources/js/components/EntityForm';
import {EntityFormConfig} from '../../../src/main/resources/META-INF/resources/js/types';

const mockUseFormState = jest.fn();

jest.mock(
	'../../../src/main/resources/META-INF/resources/js/hooks/useFormState',
	() => ({
		useFormState: (...args: unknown[]) => mockUseFormState(...args),
	})
);

jest.mock(
	'../../../src/main/resources/META-INF/resources/js/hooks/useProgress',
	() => ({
		useProgress: () => ({
			percent: 0,
			reset: jest.fn(),
			running: false,
			start: jest.fn(),
		}),
	})
);

jest.mock(
	'../../../src/main/resources/META-INF/resources/js/hooks/useApiData',
	() => ({
		useApiData: () => ({
			data: [],
			error: null,
			loading: false,
			reload: jest.fn(),
		}),
	})
);

const config: EntityFormConfig = {
	actionURL: '/ldf/user',
	entityType: 'USER',
	fields: [],
	helpText: 'help',
	icon: 'user',
	label: 'user',
};

function formStateFor(submitting: boolean) {
	return {
		endSubmit: jest.fn(),
		errors: {},
		reset: jest.fn(),
		setValue: jest.fn(),
		startSubmit: jest.fn(),
		submitting,
		validate: jest.fn().mockReturnValue(true),
		values: {},
	};
}

describe('EntityForm i18n submit button', () => {
	beforeEach(() => {
		mockUseFormState.mockReset();
	});

	it("renders the Run label via Liferay.Language.get('run') when idle", () => {
		mockUseFormState.mockReturnValue(formStateFor(false));

		render(
			<EntityForm
				actionResourceURLs={{'/ldf/user': '/o/user'}}
				config={config}
				dataResourceURL="/o/data"
				progressResourceURL="/o/progress"
			/>
		);

		expect(
			screen.queryByText(Liferay.Language.get('run'))
		).not.toBeNull();
		expect(
			screen.queryByText(Liferay.Language.get('running'))
		).toBeNull();
	});

	it("renders the Running label via Liferay.Language.get('running') when submitting", () => {
		mockUseFormState.mockReturnValue(formStateFor(true));

		render(
			<EntityForm
				actionResourceURLs={{'/ldf/user': '/o/user'}}
				config={config}
				dataResourceURL="/o/data"
				progressResourceURL="/o/progress"
			/>
		);

		expect(
			screen.queryByText(Liferay.Language.get('running'))
		).not.toBeNull();
		expect(screen.queryByText(Liferay.Language.get('run'))).toBeNull();
	});
});
