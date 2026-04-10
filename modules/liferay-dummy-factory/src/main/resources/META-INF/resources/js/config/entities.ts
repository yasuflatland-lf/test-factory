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

const USER_CONFIG: EntityFormConfig = {
	actionURL: '/ldf/user',
	entityType: ENTITY_TYPES.USERS,
	fields: [
		{
			label: 'number-of-users',
			name: 'count',
			required: true,
			type: 'number',
			validators: [
				{message: 'please-enter-a-valid-number', type: 'digits'},
				{message: 'value-must-be-greater-than-0', type: 'min', value: 1},
			],
		},
		{
			label: 'base-user-name',
			name: 'baseName',
			required: true,
			type: 'text',
		},
		{
			advanced: true,
			defaultValue: 'liferay.com',
			label: 'email-domain',
			name: 'emailDomain',
			required: false,
			type: 'text',
		},
		{
			advanced: true,
			defaultValue: 'test',
			label: 'password',
			name: 'password',
			required: false,
			type: 'text',
		},
		{
			advanced: true,
			defaultValue: true,
			label: 'male',
			name: 'male',
			required: false,
			type: 'toggle',
		},
		{
			advanced: true,
			defaultValue: '',
			label: 'job-title',
			name: 'jobTitle',
			required: false,
			type: 'text',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/organizations',
			defaultValue: '',
			label: 'organizations',
			name: 'organizationIds',
			required: false,
			type: 'multiselect',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/roles',
			defaultValue: '',
			label: 'roles',
			name: 'roleIds',
			required: false,
			type: 'multiselect',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/user-groups',
			defaultValue: '',
			label: 'user-groups',
			name: 'userGroupIds',
			required: false,
			type: 'multiselect',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/site-roles',
			defaultValue: '',
			label: 'site-roles',
			name: 'siteRoleIds',
			required: false,
			type: 'multiselect',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/org-roles',
			defaultValue: '',
			label: 'organization-roles',
			name: 'orgRoleIds',
			required: false,
			type: 'multiselect',
		},
	],
	helpText: 'user-help-text',
	icon: 'user',
	label: 'users',
};

const ROLE_CONFIG: EntityFormConfig = {
	actionURL: '/ldf/role',
	entityType: ENTITY_TYPES.ROLES,
	fields: [
		{
			label: 'number-of-roles',
			name: 'count',
			required: true,
			type: 'number',
			validators: [
				{message: 'please-enter-a-valid-number', type: 'digits'},
				{message: 'value-must-be-greater-than-0', type: 'min', value: 1},
			],
		},
		{
			label: 'base-role-name',
			name: 'baseName',
			required: true,
			type: 'text',
		},
		{
			defaultValue: 'regular',
			label: 'role-type',
			name: 'roleType',
			options: [
				{label: 'regular', value: 'regular'},
				{label: 'site', value: 'site'},
				{label: 'organization', value: 'organization'},
				{label: 'asset-library', value: 'depot'},
				{label: 'account', value: 'account'},
				{label: 'publications', value: 'publications'},
				{label: 'provider', value: 'provider'},
			],
			required: true,
			type: 'select',
		},
		{
			advanced: true,
			defaultValue: '',
			label: 'description',
			name: 'description',
			required: false,
			type: 'textarea',
		},
	],
	helpText: 'role-help-text',
	icon: 'roles',
	label: 'roles',
};

export const ENTITY_CONFIGS: Partial<Record<EntityType, EntityFormConfig>> = {
	[ENTITY_TYPES.ORGANIZATION]: ORGANIZATION_CONFIG,
	[ENTITY_TYPES.ROLES]: ROLE_CONFIG,
	[ENTITY_TYPES.USERS]: USER_CONFIG,
};

export function getEntityConfig(entityType: EntityType): EntityFormConfig | undefined {
	return ENTITY_CONFIGS[entityType];
}
