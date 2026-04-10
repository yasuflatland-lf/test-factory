import {ApiResponse} from '../types';

export async function fetchResource<T>(
	resourceURL: string,
	params?: Record<string, string>
): Promise<ApiResponse<T>> {
	const url = new URL(resourceURL, window.location.origin);

	if (params) {
		Object.entries(params).forEach(([key, value]) => {
			url.searchParams.append(key, value);
		});
	}

	try {
		const response = await fetch(url.toString(), {
			credentials: 'include',
			method: 'GET',
		});

		const data = await response.json();

		if (data.error) {
			return {error: data.error, success: false};
		}

		return {data, success: true};
	}
	catch (error) {
		return {
			error: error instanceof Error ? error.message : 'Unknown error',
			success: false,
		};
	}
}

export async function submitResource<T>(
	resourceURL: string,
	params: Record<string, string>
): Promise<ApiResponse<T>> {
	const url = new URL(resourceURL, window.location.origin);

	Object.entries(params).forEach(([key, value]) => {
		url.searchParams.append(key, value);
	});

	try {
		const response = await fetch(url.toString(), {
			credentials: 'include',
			method: 'GET',
		});

		const data = await response.json();

		if (data.error) {
			return {error: data.error, success: false};
		}

		return {data, success: true};
	}
	catch (error) {
		return {
			error: error instanceof Error ? error.message : 'Unknown error',
			success: false,
		};
	}
}
