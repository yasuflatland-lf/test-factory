import {ENTITY_TYPES, EntityType} from './constants';
import {EntityFormConfig} from '../types';

const ORGANIZATION_CONFIG: EntityFormConfig = {
	actionURL: '/ldf/org',
	entityType: ENTITY_TYPES.ORGANIZATION,
	fields: [
		{
			label: 'number-of-organizations',
			name: 'count',
			required: true,
			type: 'number',
			validators: [
				{message: 'please-enter-a-valid-number', type: 'digits'},
				{message: 'value-must-be-greater-than-0', type: 'min', value: 1},
			],
		},
		{
			label: 'base-organization-name',
			name: 'baseName',
			required: true,
			type: 'text',
		},
		{
			dataSource: '/ldf/data/organizations',
			defaultValue: '0',
			label: 'parent-organization',
			name: 'parentOrganizationId',
			required: false,
			type: 'select',
		},
		{
			defaultValue: false,
			label: 'create-organization-site',
			name: 'site',
			required: false,
			type: 'toggle',
		},
	],
	helpText: 'organization-help-text',
	icon: 'organizations',
	label: 'organizations',
};

export const ENTITY_CONFIGS: Partial<Record<EntityType, EntityFormConfig>> = {
	[ENTITY_TYPES.ORGANIZATION]: ORGANIZATION_CONFIG,
};

export function getEntityConfig(entityType: EntityType): EntityFormConfig | undefined {
	return ENTITY_CONFIGS[entityType];
}
