import {render, screen} from '@testing-library/react';

import ResultAlert from '../../../src/main/resources/META-INF/resources/js/components/ResultAlert';

describe('ResultAlert i18n', () => {
	const noop = () => {
		// no-op
	};

	it('renders the success i18n message when type is success', () => {
		render(
			<ResultAlert
				message={Liferay.Language.get('execution-completed-successfully')}
				onDismiss={noop}
				type="success"
			/>
		);

		expect(
			screen.queryByText(
				Liferay.Language.get('execution-completed-successfully')
			)
		).not.toBeNull();
	});

	it('renders the failed i18n message when type is danger', () => {
		render(
			<ResultAlert
				message={Liferay.Language.get('execution-failed')}
				onDismiss={noop}
				type="danger"
			/>
		);

		expect(
			screen.queryByText(Liferay.Language.get('execution-failed'))
		).not.toBeNull();
	});

	it('renders the partial execution i18n message when type is warning', () => {
		const partialText = Liferay.Language.get('partial-execution');

		render(
			<ResultAlert
				message={partialText}
				onDismiss={noop}
				type="warning"
			/>
		);

		expect(screen.queryByText(partialText)).not.toBeNull();
	});
});
