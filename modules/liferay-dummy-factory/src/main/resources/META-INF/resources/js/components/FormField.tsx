import {FieldDefinition, SelectOption} from '../types';

interface FormFieldProps {
	error?: string;
	field: FieldDefinition;
	onChange: (name: string, value: string) => void;
	options?: SelectOption[];
	value: string;
}

function FieldLabel({field}: {field: FieldDefinition}) {
	return (
		<label htmlFor={field.name}>
			{Liferay.Language.get(field.label)}

			{field.required && <span className="reference-mark text-warning">*</span>}
		</label>
	);
}

function FieldError({error}: {error?: string}) {
	if (!error) {
		return null;
	}

	return <div className="form-feedback-item">{error}</div>;
}

function FormField({error, field, onChange, options, value}: FormFieldProps) {
	const resolvedOptions = options || field.options || [];

	if (field.type === 'toggle') {
		return (
			<div className="form-group">
				<label className="toggle-switch" htmlFor={field.name}>
					<input
						checked={value === 'true'}
						className="toggle-switch-check"
						id={field.name}
						onChange={(e) =>
							onChange(field.name, String(e.target.checked))
						}
						type="checkbox"
					/>

					<span aria-hidden="true" className="toggle-switch-bar">
						<span className="toggle-switch-handle" />
					</span>

					<span className="toggle-switch-text">
						{Liferay.Language.get(field.label)}
					</span>
				</label>
			</div>
		);
	}

	if (field.type === 'multiselect') {
		const selectedValues = value ? value.split(',').filter(Boolean) : [];

		return (
			<div className={`form-group ${error ? 'has-error' : ''}`}>
				<FieldLabel field={field} />

				<select
					className="form-control"
					id={field.name}
					multiple
					onChange={(e) => {
						const selected = Array.from(
							e.target.selectedOptions,
							(opt) => opt.value
						);

						onChange(field.name, selected.join(','));
					}}
					value={selectedValues}
				>
					{resolvedOptions.map((opt) => (
						<option key={opt.value} value={opt.value}>
							{opt.label}
						</option>
					))}
				</select>

				<FieldError error={error} />
			</div>
		);
	}

	if (field.type === 'select') {
		return (
			<div className={`form-group ${error ? 'has-error' : ''}`}>
				<FieldLabel field={field} />

				<select
					className="form-control"
					id={field.name}
					onChange={(e) => onChange(field.name, e.target.value)}
					value={value}
				>
					<option value="">{Liferay.Language.get('select')}</option>

					{resolvedOptions.map((opt) => (
						<option key={opt.value} value={opt.value}>
							{opt.label}
						</option>
					))}
				</select>

				<FieldError error={error} />
			</div>
		);
	}

	return (
		<div className={`form-group ${error ? 'has-error' : ''}`}>
			<FieldLabel field={field} />

			<input
				className="form-control"
				id={field.name}
				onChange={(e) => onChange(field.name, e.target.value)}
				type={field.type === 'number' ? 'number' : 'text'}
				value={value}
			/>

			<FieldError error={error} />
		</div>
	);
}

export default FormField;
