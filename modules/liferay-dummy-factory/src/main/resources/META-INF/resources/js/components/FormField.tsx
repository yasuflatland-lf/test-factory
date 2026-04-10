import {FieldDefinition} from '../types';

interface FormFieldProps {
	error?: string;
	field: FieldDefinition;
	onChange: (name: string, value: string) => void;
	options?: Array<{label: string; value: string}>;
	value: string;
}

function FormField({error, field, onChange, options, value}: FormFieldProps) {
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

	if (field.type === 'select') {
		return (
			<div className={`form-group ${error ? 'has-error' : ''}`}>
				<label htmlFor={field.name}>
					{Liferay.Language.get(field.label)}

					{field.required && <span className="reference-mark text-warning">*</span>}
				</label>

				<select
					className="form-control"
					id={field.name}
					onChange={(e) => onChange(field.name, e.target.value)}
					value={value}
				>
					<option value="">{Liferay.Language.get('select')}</option>

					{(options || field.options || []).map((opt) => (
						<option key={opt.value} value={opt.value}>
							{opt.label}
						</option>
					))}
				</select>

				{error && <div className="form-feedback-item">{error}</div>}
			</div>
		);
	}

	return (
		<div className={`form-group ${error ? 'has-error' : ''}`}>
			<label htmlFor={field.name}>
				{Liferay.Language.get(field.label)}

				{field.required && <span className="reference-mark text-warning">*</span>}
			</label>

			<input
				className="form-control"
				id={field.name}
				onChange={(e) => onChange(field.name, e.target.value)}
				type={field.type === 'number' ? 'number' : 'text'}
				value={value}
			/>

			{error && <div className="form-feedback-item">{error}</div>}
		</div>
	);
}

export default FormField;
