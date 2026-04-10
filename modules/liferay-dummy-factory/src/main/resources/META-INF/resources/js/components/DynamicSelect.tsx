import {FieldDefinition} from '../types';
import {useApiData} from '../hooks/useApiData';

import FormField from './FormField';

interface DynamicSelectProps {
	dataResourceURL?: string;
	error?: string;
	field: FieldDefinition;
	onChange: (name: string, value: string) => void;
	value: string;
}

function DynamicSelect({dataResourceURL, error, field, onChange, value}: DynamicSelectProps) {
	const {data, loading} = useApiData(dataResourceURL, field.dataSource);

	if (loading) {
		return (
			<div className="form-group">
				<label htmlFor={field.name}>
					{Liferay.Language.get(field.label)}
				</label>

				<div className="loading-animation loading-animation-sm" />
			</div>
		);
	}

	return (
		<FormField
			error={error}
			field={field}
			onChange={onChange}
			options={data}
			value={value}
		/>
	);
}

export default DynamicSelect;
