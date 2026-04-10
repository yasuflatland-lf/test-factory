export type FieldType = 'number' | 'text' | 'select' | 'multiselect' | 'toggle' | 'textarea';

export interface SelectOption {
	label: string;
	value: string;
}

export interface FieldDefinition {
	advanced?: boolean;
	dataSource?: string;
	defaultValue?: unknown;
	label: string;
	name: string;
	options?: SelectOption[];
	required: boolean;
	type: FieldType;
	validators?: Validator[];
}

export interface Validator {
	message: string;
	type: 'min' | 'max' | 'required' | 'digits';
	value?: number;
}

export interface EntityFormConfig {
	actionURL: string;
	entityType: string;
	fields: FieldDefinition[];
	helpText: string;
	icon: string;
	label: string;
}

export type ApiResponse<T = unknown> =
	| {success: true; data: T}
	| {success: false; error: string};
