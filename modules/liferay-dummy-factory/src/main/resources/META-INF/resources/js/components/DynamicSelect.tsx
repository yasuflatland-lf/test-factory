import {FieldDefinition} from '../types';
import {useApiData} from '../hooks/useApiData';

import FormField from './FormField';

interface DynamicSelectProps {
	dataResourceURL?: string;
	dependsOnValue?: string;
	error?: string;
	field: FieldDefinition;
	onChange: (name: string, value: string) => void;
	value: string;
}

function DynamicSelect({dataResourceURL, dependsOnValue, error, field, onChange, value}: DynamicSelectProps) {
	const extraParams = field.dependsOn
		? {[field.dependsOn.paramName]: dependsOnValue ?? ''}
		: undefined;

	const {data, loading} = useApiData(dataResourceURL, field.dataSource, extraParams);

	if (field.dependsOn && !dependsOnValue) {
		return (
			<div className="form-group">
				<label htmlFor={field.name}>
					{Liferay.Language.get(field.label)}
				</label>

				<select
					className="form-control"
					disabled
					id={field.name}
				>
					<option>
						{Liferay.Language.get('please-select-parent-first')}
					</option>
				</select>
			</div>
		);
	}

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
