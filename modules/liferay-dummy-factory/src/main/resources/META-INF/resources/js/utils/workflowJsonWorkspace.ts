import {
	ApiResponse,
	WorkflowExecuteResponse,
	WorkflowPlanResponse,
	WorkflowRequestPayload,
	WorkflowSchemaResponse,
	WorkflowStepInput,
} from '../types';
import * as api from './api';

export const WORKFLOW_JSON_ENTITY_TYPE = 'WORKFLOW_JSON';

export interface WorkflowJsonSelectorEntry {
	descriptionKey: string;
	entityType: typeof WORKFLOW_JSON_ENTITY_TYPE;
	labelKey: string;
}

export type WorkflowJsonRequest = WorkflowRequestPayload;

export type WorkflowJsonStep = WorkflowStepInput;

export interface WorkflowJsonSample {
	descriptionKey: string;
	id: string;
	json: string;
	operations: string[];
	titleKey: string;
}

type WorkflowJsonParseResult =
	| {ok: true; value: WorkflowJsonRequest}
	| {error: string; ok: false};

interface ClipboardLike {
	writeText(text: string): Promise<void>;
}

const _selectorEntry: WorkflowJsonSelectorEntry = {
	descriptionKey: 'workflow-json-help-text',
	entityType: WORKFLOW_JSON_ENTITY_TYPE,
	labelKey: 'workflow-json',
};

const _sampleDefinitions: Array<Omit<WorkflowJsonSample, 'json'> & {request: WorkflowJsonRequest}> = [
	{
		descriptionKey: 'workflow-json-sample-site-description',
		id: 'site-and-page',
		operations: ['site.create', 'layout.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createSite',
					idempotencyKey: 'sample-site-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'site.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'Sample Site'},
					],
				},
				{
					id: 'createPage',
					idempotencyKey: 'sample-page-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'layout.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'Welcome'},
						{name: 'groupId', from: 'steps.createSite.items[0].groupId'},
						{name: 'type', value: 'portlet'},
					],
				},
			],
			workflowId: 'sample-site-and-page',
		},
		titleKey: 'workflow-json-sample-site-title',
	},
	{
		descriptionKey: 'workflow-json-sample-company-description',
		id: 'company-user-organization',
		operations: ['company.create', 'user.create', 'organization.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createCompany',
					idempotencyKey: 'sample-company-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'company.create',
					params: [
						{name: 'count', value: 1},
						{name: 'webId', value: 'sample-workflow-company'},
						{name: 'virtualHostname', value: 'sample-workflow-company.local'},
					],
				},
				{
					id: 'createUser',
					idempotencyKey: 'sample-user-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'user.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'Sample Workflow User'},
						{name: 'emailDomain', value: 'example.com'},
					],
				},
				{
					id: 'createOrganization',
					idempotencyKey: 'sample-org-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'organization.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'Sample Workflow Organization'},
					],
				},
			],
			workflowId: 'sample-company-user-organization',
		},
		titleKey: 'workflow-json-sample-company-title',
	},
	{
		descriptionKey: 'workflow-json-sample-taxonomy-description',
		id: 'vocabulary-and-category',
		operations: ['vocabulary.create', 'category.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createVocabulary',
					idempotencyKey: 'sample-vocabulary-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'vocabulary.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'Sample Vocabulary'},
						{name: 'groupId', value: 0},
					],
				},
				{
					id: 'createCategory',
					idempotencyKey: 'sample-category-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'category.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'Sample Category'},
						{name: 'vocabularyId', from: 'steps.createVocabulary.items[0].vocabularyId'},
					],
				},
			],
			workflowId: 'sample-vocabulary-and-category',
		},
		titleKey: 'workflow-json-sample-taxonomy-title',
	},
];

function _parseWorkflowJson(jsonText: string): WorkflowJsonParseResult {
	let parsed: unknown;

	try {
		parsed = JSON.parse(jsonText);
	}
	catch {
		return {
			error: Liferay.Language.get('workflow-json-invalid-json'),
			ok: false,
		};
	}

	if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
		return {
			error: Liferay.Language.get('workflow-json-root-object-required'),
			ok: false,
		};
	}

	const candidate = parsed as WorkflowJsonRequest;

	if (!candidate.schemaVersion || typeof candidate.schemaVersion !== 'string') {
		return {
			error: Liferay.Language.get('workflow-json-schema-version-required'),
			ok: false,
		};
	}

	if (candidate.schemaVersion.trim() !== '1.0') {
		return {
			error: 'Schema version must be 1.0.',
			ok: false,
		};
	}

	if (
		candidate.workflowId !== undefined &&
		typeof candidate.workflowId !== 'string'
	) {
		return {
			error: Liferay.Language.get('workflow-json-workflow-id-required'),
			ok: false,
		};
	}

	if (!Array.isArray(candidate.steps) || candidate.steps.length === 0) {
		return {
			error: Liferay.Language.get('workflow-json-steps-required'),
			ok: false,
		};
	}

	if (
		candidate.input !== undefined &&
		(!candidate.input ||
			Array.isArray(candidate.input) ||
			typeof candidate.input !== 'object')
	) {
		return {
			error: Liferay.Language.get('workflow-json-input-must-be-object'),
			ok: false,
		};
	}

	for (const [index, step] of candidate.steps.entries()) {
		if (!step || Array.isArray(step) || typeof step !== 'object') {
			return {
				error: `Workflow step ${index + 1} must be an object.`,
				ok: false,
			};
		}

		const workflowStep = step as WorkflowJsonStep;

		if (!workflowStep.id || typeof workflowStep.id !== 'string') {
			return {
				error: `Workflow step ${index + 1} id is required.`,
				ok: false,
			};
		}

		if (!workflowStep.operation || typeof workflowStep.operation !== 'string') {
			return {
				error: `Workflow step ${index + 1} operation is required.`,
				ok: false,
			};
		}

		if (
			!workflowStep.idempotencyKey ||
			typeof workflowStep.idempotencyKey !== 'string'
		) {
			return {
				error: `Workflow step ${index + 1} idempotencyKey is required.`,
				ok: false,
			};
		}

		if (
			workflowStep.params !== undefined &&
			!Array.isArray(workflowStep.params)
		) {
			return {
				error: `Workflow step ${index + 1} params must be an array when provided.`,
				ok: false,
			};
		}

		if (Array.isArray(workflowStep.params)) {
			for (const [paramIndex, param] of workflowStep.params.entries()) {
				if (!param || Array.isArray(param) || typeof param !== 'object') {
					return {
						error: `Workflow step ${index + 1} parameter ${paramIndex + 1} must be an object.`,
						ok: false,
					};
				}

				const workflowParam = param as Record<string, unknown>;

				if (
					!workflowParam.name ||
					typeof workflowParam.name !== 'string'
				) {
					return {
						error: `Workflow step ${index + 1} parameter ${paramIndex + 1} requires a name.`,
						ok: false,
					};
				}

				if (
					workflowParam.from === undefined &&
					workflowParam.value === undefined
				) {
					return {
						error: `Workflow step ${index + 1} parameter ${paramIndex + 1} must define value or from.`,
						ok: false,
					};
				}

				if (
					workflowParam.from !== undefined &&
					typeof workflowParam.from !== 'string'
				) {
					return {
						error: `Workflow step ${index + 1} parameter ${paramIndex + 1} reference must be a string.`,
						ok: false,
					};
				}
			}
		}

		if (workflowStep.onError !== undefined) {
			if (
				!workflowStep.onError ||
				Array.isArray(workflowStep.onError) ||
				typeof workflowStep.onError !== 'object'
			) {
				return {
					error: `Workflow step ${index + 1} onError must be an object when provided.`,
					ok: false,
				};
			}

			const onError = workflowStep.onError as Record<string, unknown>;

			if (onError.policy !== undefined && onError.policy !== 'FAIL_FAST') {
				return {
					error: `Workflow step ${index + 1} onError.policy must be FAIL_FAST.`,
					ok: false,
				};
			}
		}
	}

	return {ok: true, value: candidate};
}

async function _submitWorkflowJson<T>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	const parsed = _parseWorkflowJson(jsonText);

	if (!parsed.ok) {
		return {
			error: parsed.error,
			success: false,
		};
	}

	const postWorkflowJson =
		'postJsonResource' in api ? api.postJsonResource : api.postResource;

	return postWorkflowJson<T>(resourceURL, parsed.value);
}

export async function copyWorkflowJsonText(
	jsonText: string,
	clipboard?: ClipboardLike
): Promise<void> {
	const targetClipboard =
		clipboard ??
		(typeof navigator !== 'undefined' ? navigator.clipboard : undefined);

	if (!targetClipboard || typeof targetClipboard.writeText !== 'function') {
		throw new Error('Clipboard API is unavailable.');
	}

	await targetClipboard.writeText(jsonText);
}

export async function downloadWorkflowJsonSchema<T = WorkflowSchemaResponse>(
	resourceURL: string
): Promise<ApiResponse<T>> {
	return api.fetchResource<T>(resourceURL);
}

export function getWorkflowJsonSample(sampleId: string): WorkflowJsonSample | null {
	return (
		getWorkflowJsonSamples().find((sample) => sample.id === sampleId) ?? null
	);
}

export function getWorkflowJsonSamples(): WorkflowJsonSample[] {
	return _sampleDefinitions.map(({request, ...sample}) => ({
		...sample,
		json: JSON.stringify(request, null, 2),
	}));
}

export function getWorkflowJsonSelectorEntry(): WorkflowJsonSelectorEntry {
	return _selectorEntry;
}

export function loadWorkflowJsonSample(sampleId: string): string | null {
	return getWorkflowJsonSample(sampleId)?.json ?? null;
}

export function validateWorkflowJsonText(
	jsonText: string
): WorkflowJsonParseResult {
	return _parseWorkflowJson(jsonText);
}

export async function validateWorkflowJson<T = WorkflowPlanResponse>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	return _submitWorkflowJson<T>(resourceURL, jsonText);
}

export async function planWorkflowJson<T = WorkflowPlanResponse>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	return _submitWorkflowJson<T>(resourceURL, jsonText);
}

export async function executeWorkflowJson<T = WorkflowExecuteResponse>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	return _submitWorkflowJson<T>(resourceURL, jsonText);
}
