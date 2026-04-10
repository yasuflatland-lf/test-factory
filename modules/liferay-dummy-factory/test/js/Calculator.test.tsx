import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';

import Calculator from '../../src/main/resources/META-INF/resources/js/Calculator';

describe('Calculator', () => {
	beforeEach(() => {
		global.fetch = jest.fn();
	});

	afterEach(() => {
		jest.restoreAllMocks();
	});

	it('renders the calculator form', () => {
		render(<Calculator calculateURL="/test/calculate" />);

		expect(screen.getByLabelText('number-1')).toBeInTheDocument();
		expect(screen.getByLabelText('number-2')).toBeInTheDocument();
		expect(screen.getByLabelText('operator')).toBeInTheDocument();
		expect(screen.getByRole('button', {name: 'calculate'})).toBeInTheDocument();
	});

	it('calculates 10 + 5 = 15', async () => {
		(global.fetch as jest.Mock).mockResolvedValueOnce({
			json: () => Promise.resolve({result: 15}),
		});

		render(<Calculator calculateURL="/test/calculate" />);

		await userEvent.clear(screen.getByLabelText('number-1'));
		await userEvent.type(screen.getByLabelText('number-1'), '10');
		await userEvent.clear(screen.getByLabelText('number-2'));
		await userEvent.type(screen.getByLabelText('number-2'), '5');

		await userEvent.click(screen.getByRole('button', {name: 'calculate'}));

		await waitFor(() => {
			expect(screen.getByText(/15/)).toBeInTheDocument();
		});

		expect(global.fetch).toHaveBeenCalledWith(
			expect.stringContaining('/test/calculate'),
			expect.objectContaining({credentials: 'include', method: 'GET'})
		);
	});

	it('displays error on division by zero', async () => {
		(global.fetch as jest.Mock).mockResolvedValueOnce({
			json: () => Promise.resolve({error: 'Division by zero'}),
		});

		render(<Calculator calculateURL="/test/calculate" />);

		await userEvent.clear(screen.getByLabelText('number-1'));
		await userEvent.type(screen.getByLabelText('number-1'), '10');
		await userEvent.selectOptions(screen.getByLabelText('operator'), '/');
		await userEvent.clear(screen.getByLabelText('number-2'));
		await userEvent.type(screen.getByLabelText('number-2'), '0');

		await userEvent.click(screen.getByRole('button', {name: 'calculate'}));

		await waitFor(() => {
			expect(screen.getByText('Division by zero')).toBeInTheDocument();
		});
	});
});
