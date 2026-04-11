import {useState} from 'react';

import {EntityFormConfig, FieldDefinition} from '../types';
import {useFormState} from '../hooks/useFormState';
import {useProgress} from '../hooks/useProgress';
import {postResource} from '../utils/api';
import AdvancedOptions from './AdvancedOptions';
import DynamicSelect from './DynamicSelect';
import FormField from './FormField';
import ProgressBar from './ProgressBar';
import ResultAlert from './ResultAlert';

interface EntityFormProps {
	actionResourceURLs: Record<string, string>;
	config: EntityFormConfig;
	dataResourceURL: string;
	progressResourceURL: string;
}

function EntityForm({actionResourceURLs, config, dataResourceURL, progressResourceURL}: EntityFormProps) {
	const {endSubmit, errors, setValue, startSubmit, submitting, validate, values} =
		useFormState(config.fields);
	const [result, setResult] = useState<{message: string; type: 'success' | 'danger'} | null>(null);
	const {percent, running} = useProgress(progressResourceURL);

	const isFieldVisible = (field: FieldDefinition): boolean => {
		if (!field.visibleWhen) {
			return true;
		}

		const controlValue = values[field.visibleWhen.field] || '';
		const allowedValues = Array.isArray(field.visibleWhen.value)
			? field.visibleWhen.value
			: [field.visibleWhen.value];

		return allowedValues.includes(String(controlValue));
	};

	const requiredFields = config.fields.filter((f) => !f.advanced);
	const advancedFields = config.fields.filter((f) => f.advanced);

	const handleSubmit = async () => {
		const visibleFields = config.fields.filter(isFieldVisible);

		if (!validate(visibleFields)) {
			return;
		}

		startSubmit();
		setResult(null);

		const actionURL = actionResourceURLs[config.actionURL];

		if (!actionURL) {
			setResult({
				message: `Missing resource URL for ${config.actionURL}`,
				type: 'danger',
			});
			endSubmit();
			return;
		}

		const submitValues: Record<string, string | number | boolean | number[]> = {...values};

		for (const field of config.fields) {
			if (field.type === 'multiselect' && submitValues[field.name]) {
				submitValues[field.name] = String(submitValues[field.name])
					.split(',')
					.filter(Boolean)
					.map(Number);
			}
		}

		const response = await postResource(actionURL, submitValues);

		endSubmit();

		if (response.success) {
			setResult({
				message: Liferay.Language.get('execution-completed-successfully'),
				type: 'success',
			});
		}
		else {
			setResult({
				message: response.error,
				type: 'danger',
			});
		}
	};

	const renderField = (field: FieldDefinition) => {
		if (field.dataSource) {
			const dependsOnValue = field.dependsOn
				? String(values[field.dependsOn.field] || '')
				: undefined;

			return (
				<DynamicSelect
					dataResourceURL={dataResourceURL}
					dependsOnValue={dependsOnValue}
					error={errors[field.name]}
					field={field}
					key={field.name + (dependsOnValue || '')}
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
				{requiredFields.filter(isFieldVisible).map(renderField)}

				{advancedFields.length > 0 && (
					<AdvancedOptions>
						{advancedFields.filter(isFieldVisible).map(renderField)}
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
