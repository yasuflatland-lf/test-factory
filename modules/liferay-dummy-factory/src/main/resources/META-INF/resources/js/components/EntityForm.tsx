import {useState} from 'react';

import {EntityFormConfig, FieldDefinition} from '../types';
import {useFormState} from '../hooks/useFormState';
import {useProgress} from '../hooks/useProgress';
import {fetchResource} from '../utils/api';
import AdvancedOptions from './AdvancedOptions';
import DynamicSelect from './DynamicSelect';
import FormField from './FormField';
import ProgressBar from './ProgressBar';
import ResultAlert from './ResultAlert';

interface EntityFormProps {
	actionResourceURL: string;
	config: EntityFormConfig;
	dataResourceURL: string;
	progressResourceURL: string;
}

function EntityForm({actionResourceURL, config, dataResourceURL, progressResourceURL}: EntityFormProps) {
	const {endSubmit, errors, setValue, startSubmit, submitting, validate, values} =
		useFormState(config.fields);
	const [result, setResult] = useState<{message: string; type: 'success' | 'danger'} | null>(null);
	const {percent, running} = useProgress(progressResourceURL);

	const requiredFields = config.fields.filter((f) => !f.advanced);
	const advancedFields = config.fields.filter((f) => f.advanced);

	const handleSubmit = async () => {
		if (!validate()) {
			return;
		}

		startSubmit();
		setResult(null);

		const response = await fetchResource(actionResourceURL, values);

		endSubmit();

		if (response.success) {
			setResult({
				message: Liferay.Language.get('execution-completed-successfully'),
				type: 'success',
			});
		}
		else {
			setResult({
				message: response.error || Liferay.Language.get('an-error-occurred'),
				type: 'danger',
			});
		}
	};

	const renderField = (field: FieldDefinition) => {
		if (field.dataSource) {
			return (
				<DynamicSelect
					dataResourceURL={dataResourceURL}
					error={errors[field.name]}
					field={field}
					key={field.name}
					onChange={setValue}
					value={values[field.name] || ''}
				/>
			);
		}

		return (
			<FormField
				error={errors[field.name]}
				field={field}
				key={field.name}
				onChange={setValue}
				value={values[field.name] || ''}
			/>
		);
	};

	return (
		<div className="sheet">
			<div className="sheet-header">
				<h2>{Liferay.Language.get(config.label)}</h2>
			</div>

			<div className="sheet-section">
				{requiredFields.map(renderField)}

				{advancedFields.length > 0 && (
					<AdvancedOptions>
						{advancedFields.map(renderField)}
					</AdvancedOptions>
				)}
			</div>

			<ProgressBar percent={percent} running={running} />

			<div className="sheet-footer">
				<button
					className="btn btn-primary"
					disabled={submitting}
					onClick={handleSubmit}
					type="button"
				>
					{submitting
						? Liferay.Language.get('running')
						: Liferay.Language.get('run')}
				</button>
			</div>

			{result && (
				<ResultAlert
					message={result.message}
					onDismiss={() => setResult(null)}
					type={result.type}
				/>
			)}
		</div>
	);
}

export default EntityForm;
