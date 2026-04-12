export type FieldType = 'number' | 'text' | 'file' | 'select' | 'multiselect' | 'toggle' | 'textarea';

export const FIELD_GROUPS = ['identity', 'generator', 'membership', 'layout', 'content'] as const;
export type FieldGroup = typeof FIELD_GROUPS[number];

export interface SelectOption {
	label: string;
	value: string;
}

export interface FieldDefinition {
	advanced?: boolean;
	dataSource?: string;
	group?: FieldGroup;
	defaultValue?: unknown;
	dependsOn?: {
		field: string;
		paramName: string;
	};
	label: string;
	name: string;
	options?: SelectOption[];
	required: boolean;
	type: FieldType;
	validators?: Validator[];
	visibleWhen?: {
		field: string;
		value: string | string[];
	};
}

export interface PerSiteResult {
	groupId: number;
	siteName: string;
	created: number;
	failed: number;
	error?: string;
}

export interface MultiSiteResult {
	ok: boolean;
	totalRequested: number;
	totalCreated: number;
	perSite: PerSiteResult[];
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
	| {success: false; data?: T; error: string};
