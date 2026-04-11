import {render, screen} from '@testing-library/react';

import ResultAlert from '../../../src/main/resources/META-INF/resources/js/components/ResultAlert';

describe('ResultAlert i18n', () => {
	const noop = () => {
		// no-op
	};

	it('renders the success i18n message when type is success', () => {
		const successText = Liferay.Language.get(
			'execution-completed-successfully'
		);

		expect(successText).not.toBe('execution-completed-successfully');
		expect(successText.length).toBeGreaterThan(0);

		render(
			<ResultAlert
				message={successText}
				onDismiss={noop}
				type="success"
			/>
		);

		expect(screen.queryByText(successText)).not.toBeNull();
	});

	it('renders the failed i18n message when type is danger', () => {
		const failedText = Liferay.Language.get('execution-failed');

		expect(failedText).not.toBe('execution-failed');
		expect(failedText.length).toBeGreaterThan(0);

		render(
			<ResultAlert
				message={failedText}
				onDismiss={noop}
				type="danger"
			/>
		);

		expect(screen.queryByText(failedText)).not.toBeNull();
	});

	it('renders the partial execution i18n message when type is warning', () => {
		const partialText = Liferay.Language.get('partial-execution');

		expect(partialText).not.toBe('partial-execution');
		expect(partialText.length).toBeGreaterThan(0);

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
