import {fireEvent, render, screen, waitFor} from '@testing-library/react';

import FileUploadArea from '../../../src/main/resources/META-INF/resources/js/components/FileUploadArea';

type MockFetch = jest.Mock<Promise<Partial<Response>>, [RequestInfo, RequestInit?]>;

const mockFetch: MockFetch = jest.fn();

beforeEach(() => {
	(global as unknown as {fetch: MockFetch}).fetch = mockFetch;
	mockFetch.mockReset();
});

function renderComponent(
	overrides: Partial<React.ComponentProps<typeof FileUploadArea>> = {}
) {
	const props: React.ComponentProps<typeof FileUploadArea> = {
		groupId: '20121',
		onChange: jest.fn(),
		testId: 'file-upload',
		uploadURL: '/o/ldf/upload',
		value: '',
		...overrides,
	};

	return {...render(<FileUploadArea {...props} />), props};
}

describe('FileUploadArea i18n', () => {
	it('renders the upload-template-files label in its initial state', () => {
		renderComponent();

		expect(
			screen.queryByText(Liferay.Language.get('upload-template-files'))
		).not.toBeNull();
		expect(screen.queryByText('Upload Template Files')).not.toBeNull();
	});

	it('renders the uploading message while a file upload is in flight', async () => {
		let resolveFetch: (value: Partial<Response>) => void = () => undefined;

		mockFetch.mockImplementationOnce(
			() =>
				new Promise<Partial<Response>>((resolve) => {
					resolveFetch = resolve;
				})
		);

		const {container} = renderComponent();

		const input = container.querySelector(
			'input[type="file"]'
		) as HTMLInputElement;

		const file = new File(['hello'], 'template.txt', {type: 'text/plain'});

		fireEvent.change(input, {target: {files: [file]}});

		await waitFor(() => {
			expect(
				screen.queryByText(
					new RegExp(`^${Liferay.Language.get('uploading')}`)
				)
			).not.toBeNull();
		});

		expect(screen.queryByText(/template\.txt/)).not.toBeNull();

		resolveFetch({
			json: () =>
				Promise.resolve({fileName: 'template.txt', success: true}),
			ok: true,
		} as Partial<Response>);
	});

	it('renders the upload-failed message when the server returns an error', async () => {
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 500,
		} as Partial<Response>);

		const {container} = renderComponent();

		const input = container.querySelector(
			'input[type="file"]'
		) as HTMLInputElement;

		const file = new File(['fail'], 'broken.txt', {type: 'text/plain'});

		fireEvent.change(input, {target: {files: [file]}});

		await waitFor(() => {
			expect(
				screen.queryByText(/Server error: 500/)
			).not.toBeNull();
		});

		expect(screen.queryByText(/broken\.txt/)).not.toBeNull();
	});

	it('falls back to the upload-failed i18n string when the server response is unsuccessful without an error field', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({success: false}),
			ok: true,
		} as Partial<Response>);

		const {container} = renderComponent();

		const input = container.querySelector(
			'input[type="file"]'
		) as HTMLInputElement;

		const file = new File(['fail'], 'noerror.txt', {type: 'text/plain'});

		fireEvent.change(input, {target: {files: [file]}});

		const uploadFailedText = Liferay.Language.get('upload-failed');

		await waitFor(() => {
			expect(
				screen.queryByText(new RegExp(uploadFailedText))
			).not.toBeNull();
		});

		expect(uploadFailedText).toBe('Upload failed');
	});
});
