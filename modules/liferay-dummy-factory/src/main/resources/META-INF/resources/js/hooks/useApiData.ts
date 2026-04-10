import {useCallback, useEffect, useState} from 'react';

import {SelectOption} from '../types';
import {fetchResource} from '../utils/api';

interface UseApiDataResult {
	data: SelectOption[];
	error: string | null;
	loading: boolean;
	reload: () => void;
}

export function useApiData(
	resourceURL: string | undefined,
	dataSource: string | undefined
): UseApiDataResult {
	const [data, setData] = useState<SelectOption[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const load = useCallback(async () => {
		if (!resourceURL || !dataSource) {
			return;
		}

		setLoading(true);
		setError(null);

		const result = await fetchResource<SelectOption[]>(resourceURL, {
			type: dataSource.split('/').pop() || '',
		});

		if (result.success && result.data) {
			setData(result.data);
		}
		else {
			setError(result.error || 'Failed to load data');
		}

		setLoading(false);
	}, [resourceURL, dataSource]);

	useEffect(() => {
		load();
	}, [load]);

	return {data, error, loading, reload: load};
}
