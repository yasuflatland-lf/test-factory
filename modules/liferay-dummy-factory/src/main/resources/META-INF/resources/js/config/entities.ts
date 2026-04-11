import {ENTITY_TYPES, EntityType} from './constants';
import {EntityFormConfig, FieldDefinition} from '../types';

function createCountField(label: string): FieldDefinition {
	return {
		label,
		name: 'count',
		required: true,
		type: 'number',
		validators: [
			{message: 'please-enter-a-valid-number', type: 'digits'},
			{message: 'value-must-be-greater-than-0', type: 'min', value: 1},
		],
	};
}

function createBaseNameField(label: string): FieldDefinition {
	return {
		label,
		name: 'baseName',
		required: true,
		type: 'text',
	};
}

const ORGANIZATION_CONFIG: EntityFormConfig = {
	actionURL: '/ldf/org',
	entityType: ENTITY_TYPES.ORGANIZATION,
	fields: [
		createCountField('number-of-organizations'),
		createBaseNameField('base-organization-name'),
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
		createCountField('number-of-users'),
		createBaseNameField('base-user-name'),
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
		createCountField('number-of-roles'),
		createBaseNameField('base-role-name'),
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

const SITE_CONFIG: EntityFormConfig = {
	actionURL: '/ldf/site',
	entityType: ENTITY_TYPES.SITES,
	fields: [
		createCountField('number-of-sites'),
		createBaseNameField('base-site-name'),
		{
			defaultValue: 'open',
			label: 'membership-type',
			name: 'membershipType',
			options: [
				{label: 'open', value: 'open'},
				{label: 'restricted', value: 'restricted'},
				{label: 'private', value: 'private'},
			],
			required: true,
			type: 'select',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/sites',
			defaultValue: '0',
			label: 'parent-site',
			name: 'parentGroupId',
			required: false,
			type: 'select',
		},
		{
			advanced: true,
			dataSource: '/ldf/data/site-templates',
			defaultValue: '0',
			label: 'site-template',
			name: 'siteTemplateId',
			required: false,
			type: 'select',
		},
		{
			advanced: true,
			defaultValue: true,
			label: 'manual-membership',
			name: 'manualMembership',
			required: false,
			type: 'toggle',
		},
		{
			advanced: true,
			defaultValue: false,
			label: 'inherit-content',
			name: 'inheritContent',
			required: false,
			type: 'toggle',
		},
		{
			advanced: true,
			defaultValue: true,
			label: 'active',
			name: 'active',
			required: false,
			type: 'toggle',
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
	helpText: 'site-help-text',
	icon: 'sites',
	label: 'sites',
};

export const ENTITY_CONFIGS: Partial<Record<EntityType, EntityFormConfig>> = {
	[ENTITY_TYPES.ORGANIZATION]: ORGANIZATION_CONFIG,
	[ENTITY_TYPES.ROLES]: ROLE_CONFIG,
	[ENTITY_TYPES.SITES]: SITE_CONFIG,
	[ENTITY_TYPES.USERS]: USER_CONFIG,
};

export function getEntityConfig(entityType: EntityType): EntityFormConfig | undefined {
	return ENTITY_CONFIGS[entityType];
}
