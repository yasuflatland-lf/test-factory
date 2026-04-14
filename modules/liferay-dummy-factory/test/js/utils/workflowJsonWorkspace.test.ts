import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockFetchResource = vi.fn();
const mockPostResource = vi.fn();
const mockPostJsonResource = vi.fn();

vi.mock(
	'../../../src/main/resources/META-INF/resources/js/utils/api',
	() => ({
		fetchResource: (...args: unknown[]) => mockFetchResource(...args),
		postJsonResource: (...args: unknown[]) => mockPostJsonResource(...args),
		postResource: (...args: unknown[]) => mockPostResource(...args),
	})
);

import {
	copyWorkflowJsonText,
	downloadWorkflowJsonSchema,
	executeWorkflowJson,
	getWorkflowJsonSample,
	getWorkflowJsonSelectorEntry,
	getWorkflowJsonSamples,
	loadWorkflowJsonSample,
	planWorkflowJson,
	validateWorkflowJson,
	validateWorkflowJsonText,
	WORKFLOW_JSON_ENTITY_TYPE,
} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonWorkspace';

describe('workflowJsonWorkspace selector', () => {
	it('exposes a selector entry for the workflow json workspace', () => {
		const entry = getWorkflowJsonSelectorEntry();

		expect(entry.entityType).toBe(WORKFLOW_JSON_ENTITY_TYPE);
		expect(entry.labelKey).toBe('workflow-json');
		expect(Liferay.Language.get(entry.labelKey)).toBe('Workflow JSON');
		expect(Liferay.Language.get(entry.descriptionKey)).toBe(
			'Author, validate, plan, and execute raw workflow requests from one workspace.'
		);
	});
});

describe('workflowJsonWorkspace samples', () => {
	beforeEach(() => {
		mockFetchResource.mockReset();
		mockPostResource.mockReset();
		mockPostJsonResource.mockReset();
	});

	it('keeps sample title and description as metadata outside the json payload', () => {
		const sample = getWorkflowJsonSample('company-user-organization');

		expect(sample).not.toBeNull();
		expect(sample?.titleKey).toBe('workflow-json-sample-company-title');
		expect(Liferay.Language.get(sample!.titleKey)).toBe(
			'Create a company, user, and organization'
		);

		const parsed = JSON.parse(sample!.json);

		expect(parsed.workflowId).toBe('sample-company-user-organization');
		expect(parsed.title).toBeUndefined();
		expect(parsed.description).toBeUndefined();
	});

	it('loads a selected sample into the editor as formatted json', () => {
		const json = loadWorkflowJsonSample('site-and-page');

		expect(json).not.toBeNull();

		const parsed = JSON.parse(json!);

		expect(parsed.workflowId).toBe('sample-site-and-page');
		expect(parsed.steps).toHaveLength(2);
		expect(json).toContain('\n  "steps": [\n');
	});

	it('lists curated samples with action-oriented summaries', () => {
		const samples = getWorkflowJsonSamples();

		expect(samples.map((sample) => sample.id)).toEqual([
			'site-and-page',
			'company-user-organization',
			'vocabulary-and-category',
		]);
		expect(
			Liferay.Language.get(samples[2].descriptionKey)
		).toBe('Create a vocabulary and then create a category inside it.');
	});

	it('copies the current json text to the clipboard', async () => {
		const clipboard = {
			writeText: vi.fn().mockResolvedValue(undefined),
		};
		const sample = loadWorkflowJsonSample('vocabulary-and-category');

		await copyWorkflowJsonText(sample!, clipboard);

		expect(clipboard.writeText).toHaveBeenCalledOnce();
		expect(clipboard.writeText).toHaveBeenCalledWith(sample);
	});
});

describe('workflowJsonWorkspace validation and actions', () => {
	beforeEach(() => {
		mockFetchResource.mockReset();
		mockPostResource.mockReset();
		mockPostJsonResource.mockReset();
	});

	it('surfaces json parse errors before any server validation request', async () => {
		const result = await validateWorkflowJson('/o/ldf-workflow/plan', '{"oops"');

		expect(result).toEqual({
			error: 'Invalid JSON. Fix syntax errors before validating.',
			success: false,
		});
		expect(mockPostResource).not.toHaveBeenCalled();
	});

	it('rejects non-object json for strict validation', () => {
		const result = validateWorkflowJsonText('[]');

		expect(result).toEqual({
			error: 'Workflow JSON must be an object at the top level.',
			ok: false,
		});
	});

	it('rejects requests without workflow steps', () => {
		const result = validateWorkflowJsonText(
			JSON.stringify({
				schemaVersion: '1.0',
				steps: [],
				workflowId: 'empty-steps',
			})
		);

		expect(result).toEqual({
			error: 'At least one workflow step is required.',
			ok: false,
		});
	});

	it('downloads the schema from the configured endpoint', async () => {
		mockFetchResource.mockResolvedValueOnce({
			data: {schema: {}},
			success: true,
		});

		const result = await downloadWorkflowJsonSchema('/o/ldf-workflow/schema');

		expect(mockFetchResource).toHaveBeenCalledWith('/o/ldf-workflow/schema');
		expect(result).toEqual({data: {schema: {}}, success: true});
	});

	it('wires validate to the plan endpoint with the parsed workflow payload', async () => {
		mockPostJsonResource.mockResolvedValueOnce({
			data: {errors: []},
			success: true,
		});

		const json = loadWorkflowJsonSample('site-and-page')!;

		await validateWorkflowJson('/o/ldf-workflow/plan', json);

		expect(mockPostJsonResource).toHaveBeenCalledOnce();
		expect(mockPostJsonResource).toHaveBeenCalledWith(
			'/o/ldf-workflow/plan',
			expect.objectContaining({
				schemaVersion: '1.0',
				workflowId: 'sample-site-and-page',
			})
		);
	});

	it('wires plan to the plan endpoint with the selected sample payload', async () => {
		mockPostJsonResource.mockResolvedValueOnce({
			data: {errors: [], plan: []},
			success: true,
		});

		const json = loadWorkflowJsonSample('company-user-organization')!;

		await planWorkflowJson('/o/ldf-workflow/plan', json);

		expect(mockPostJsonResource).toHaveBeenCalledWith(
			'/o/ldf-workflow/plan',
			expect.objectContaining({
				workflowId: 'sample-company-user-organization',
			})
		);
	});

	it('wires execute to the execute endpoint with the current editor payload', async () => {
		mockPostJsonResource.mockResolvedValueOnce({
			data: {execution: {status: 'SUCCEEDED'}},
			success: true,
		});

		const json = loadWorkflowJsonSample('vocabulary-and-category')!;

		await executeWorkflowJson('/o/ldf-workflow/execute', json);

		expect(mockPostJsonResource).toHaveBeenCalledWith(
			'/o/ldf-workflow/execute',
			expect.objectContaining({
				workflowId: 'sample-vocabulary-and-category',
			})
		);
	});
});
