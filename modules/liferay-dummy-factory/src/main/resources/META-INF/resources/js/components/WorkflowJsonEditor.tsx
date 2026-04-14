import {useEffect, useMemo, useState} from 'react';

import {
	copyWorkflowJsonText,
	downloadWorkflowJsonSchema,
	executeWorkflowJson,
	getWorkflowJsonSamples,
	loadWorkflowJsonSample,
	planWorkflowJson,
	validateWorkflowJson,
	validateWorkflowJsonText,
	type WorkflowJsonSample,
} from '../utils/workflowJsonWorkspace';

type WorkflowJsonResultTone = 'danger' | 'info' | 'success' | 'warning';

interface WorkflowJsonWorkspaceResult {
	action: 'copy' | 'execute' | 'load' | 'plan' | 'schema' | 'validate';
	body: string;
	summary: string;
	title: string;
	tone: WorkflowJsonResultTone;
}

export interface WorkflowJsonEditorProps {
	errorMessage: string | null;
	executeResourceURL?: string;
	onChange: (value: string) => void;
	planResourceURL?: string;
	schemaResourceURL?: string;
	value: string;
}

const _workflowJsonSamples = getWorkflowJsonSamples();

function _stringifyValue(value: unknown): string {
	if (typeof value === 'string') {
		return value;
	}

	try {
		return JSON.stringify(value, null, 2);
	}
	catch {
		return String(value);
	}
}

function _getActionLabel(action: WorkflowJsonWorkspaceResult['action']): string {
	switch (action) {
		case 'copy':
			return Liferay.Language.get('copy-json');
		case 'execute':
			return Liferay.Language.get('execute-json');
		case 'load':
			return Liferay.Language.get('load-sample');
		case 'plan':
			return Liferay.Language.get('plan-json');
		case 'schema':
			return Liferay.Language.get('download-schema');
		case 'validate':
			return Liferay.Language.get('validate-json');
	}
}

function _getResponseBody(response: unknown): string {
	if (!response) {
		return 'No data returned.';
	}

	return _stringifyValue(response);
}

function _getResponseSummary(body: string): string {
	const normalized = body.replace(/\s+/g, ' ').trim();

	if (!normalized) {
		return 'No details returned.';
	}

	if (normalized.length <= 160) {
		return normalized;
	}

	return `${normalized.slice(0, 157)}...`;
}

function _hasValidationErrors(data: unknown): boolean {
	if (!data || typeof data !== 'object' || Array.isArray(data)) {
		return false;
	}

	const candidate = data as {errors?: unknown};

	return Array.isArray(candidate.errors) && candidate.errors.length > 0;
}

function _getResultTone(
	action: WorkflowJsonWorkspaceResult['action'],
	error: boolean,
	hasWarnings: boolean
): WorkflowJsonResultTone {
	if (error) {
		return 'danger';
	}

	if (hasWarnings) {
		return 'warning';
	}

	if (action === 'schema') {
		return 'info';
	}

	return 'success';
}

function _createResult(
	action: WorkflowJsonWorkspaceResult['action'],
	title: string,
	body: string,
	tone: WorkflowJsonResultTone
): WorkflowJsonWorkspaceResult {
	return {
		action,
		body,
		summary: _getResponseSummary(body),
		title,
		tone,
	};
}

function WorkflowJsonEditor({
	errorMessage,
	executeResourceURL,
	onChange,
	planResourceURL,
	schemaResourceURL,
	value,
}: WorkflowJsonEditorProps) {
	const [draftValue, setDraftValue] = useState(value);
	const [busyAction, setBusyAction] = useState<
		WorkflowJsonWorkspaceResult['action'] | null
	>(null);
	const [result, setResult] = useState<WorkflowJsonWorkspaceResult | null>(
		null
	);
	const [resultDetailsVisible, setResultDetailsVisible] = useState(false);
	const [selectedSampleId, setSelectedSampleId] = useState(
		_workflowJsonSamples[0]?.id ?? ''
	);

	useEffect(() => {
		setDraftValue(value);
	}, [value]);

	const selectedSample = useMemo<WorkflowJsonSample | null>(() => {
		return (
			_workflowJsonSamples.find((sample) => sample.id === selectedSampleId) ??
			_workflowJsonSamples[0] ??
			null
		);
	}, [selectedSampleId]);

	const parseResult = validateWorkflowJsonText(draftValue);
	const validationError = errorMessage ?? (parseResult.ok ? null : parseResult.error);

	function _applyValue(nextValue: string) {
		setDraftValue(nextValue);
		onChange(nextValue);
	}

	function _setTextResult(
		action: WorkflowJsonWorkspaceResult['action'],
		title: string,
		body: string,
		tone: WorkflowJsonResultTone
	) {
		setResult(_createResult(action, title, body, tone));
		setResultDetailsVisible(false);
	}

	async function _handleLoadSample() {
		if (!selectedSample) {
			_setTextResult(
				'load',
				'Sample not available',
				'No sample is currently selected.',
				'danger'
			);
			return;
		}

		const json = loadWorkflowJsonSample(selectedSample.id);

		if (!json) {
			_setTextResult(
				'load',
				'Sample not available',
				`Unable to load ${Liferay.Language.get(selectedSample.titleKey)}.`,
				'danger'
			);
			return;
		}

		_applyValue(json);

		_setTextResult(
			'load',
			`${Liferay.Language.get(selectedSample.titleKey)} loaded`,
			`Loaded ${selectedSample.id} into the editor.`,
			'success'
		);
	}

	async function _handleCopyJson() {
		try {
			await copyWorkflowJsonText(draftValue);

			_setTextResult(
				'copy',
				'JSON copied',
				'The current workflow JSON is on your clipboard.',
				'success'
			);
		}
		catch (error) {
			_setTextResult(
				'copy',
				'Copy failed',
				error instanceof Error ? error.message : 'Unable to copy JSON.',
				'danger'
			);
		}
	}

	async function _handleSchemaDownload() {
		if (!schemaResourceURL) {
			_setTextResult(
				'schema',
				'Schema unavailable',
				'No schema endpoint is configured.',
				'danger'
			);
			return;
		}

		setBusyAction('schema');

		try {
			const response = await downloadWorkflowJsonSchema(schemaResourceURL);

			if (!response.success) {
				_setTextResult(
					'schema',
					'Schema download failed',
					response.error,
					'danger'
				);
				return;
			}

			_setTextResult(
				'schema',
				'Schema downloaded',
				_getResponseBody(response.data),
				'info'
			);
		}
		finally {
			setBusyAction(null);
		}
	}

	async function _handleWorkflowAction(
		action: 'execute' | 'plan' | 'validate',
		resourceURL?: string
	) {
		const currentValidation = validateWorkflowJsonText(draftValue);

		if (!currentValidation.ok) {
			_setTextResult(
				action,
				`${_getActionLabel(action)} blocked`,
				currentValidation.error,
				'danger'
			);
			return;
		}

		if (!resourceURL) {
			_setTextResult(
				action,
				`${_getActionLabel(action)} unavailable`,
				`No ${_getActionLabel(action).toLowerCase()} endpoint is configured.`,
				'danger'
			);
			return;
		}

		setBusyAction(action);

		try {
			const response =
				action === 'execute'
					? await executeWorkflowJson(resourceURL, draftValue)
					: action === 'plan'
						? await planWorkflowJson(resourceURL, draftValue)
					: await validateWorkflowJson(resourceURL, draftValue);

			if (!response.success) {
				_setTextResult(
					action,
					`${_getActionLabel(action)} failed`,
					response.error,
					'danger'
				);
				return;
			}

			const hasWarnings = _hasValidationErrors(response.data);
			const outcomeLabel = hasWarnings ? 'completed with warnings' : 'completed';
			const tone = _getResultTone(action, false, hasWarnings);
			const body = _getResponseBody(response.data);

			_setTextResult(
				action,
				`${_getActionLabel(action)} ${outcomeLabel}`,
				body,
				tone
			);
		}
		finally {
			setBusyAction(null);
		}
	}

	const canValidate = Boolean(planResourceURL);
	const canExecute = Boolean(executeResourceURL);
	const canDownloadSchema = Boolean(schemaResourceURL);
	const isBusy = busyAction !== null;

	return (
		<section className="workflow-json-panel workflow-json-workspace">
			<div className="workflow-json-panel-header">
				<div>
					<p className="workflow-json-eyebrow">
						{Liferay.Language.get('workflow-json')}
					</p>
					<h3>{Liferay.Language.get('workflow-json-editor')}</h3>
				</div>
			</div>

			<div className="workflow-json-grid">
				<aside className="workflow-json-panel workflow-json-samples-panel">
					<p className="workflow-json-eyebrow">
						{Liferay.Language.get('workflow-json-samples')}
					</p>

					<label
						className="workflow-json-label"
						htmlFor="workflow-json-sample-select"
					>
						Sample workflow
					</label>

					<select
						className="form-control"
						data-testid="workflow-json-sample-select"
						id="workflow-json-sample-select"
						onChange={(event) => setSelectedSampleId(event.target.value)}
						value={selectedSample?.id ?? ''}
					>
						{_workflowJsonSamples.map((sample) => (
							<option key={sample.id} value={sample.id}>
								{Liferay.Language.get(sample.titleKey)}
							</option>
						))}
					</select>

					{selectedSample && (
						<div className="workflow-json-sample-meta">
							<p>{Liferay.Language.get(selectedSample.descriptionKey)}</p>

							<ul className="workflow-json-sample-operations">
								{selectedSample.operations.map((operation) => (
									<li key={operation}>{operation}</li>
								))}
							</ul>
						</div>
					)}
				</aside>

				<div className="workflow-json-panel workflow-json-editor-panel">
					<div
						className="workflow-json-action-row workflow-json-toolbar"
						data-testid="workflow-json-toolbar"
					>
						<button
							className="btn btn-primary"
							data-testid="workflow-json-load-sample"
							disabled={!selectedSample || isBusy}
							onClick={_handleLoadSample}
							type="button"
						>
							{Liferay.Language.get('load-sample')}
						</button>

						<button
							className="btn btn-secondary"
							data-testid="workflow-json-copy-json"
							disabled={isBusy}
							onClick={_handleCopyJson}
							type="button"
						>
							{Liferay.Language.get('copy-json')}
						</button>

						<button
							className="btn btn-outline-primary"
							data-testid="workflow-json-download-schema"
							disabled={!canDownloadSchema || isBusy}
							onClick={_handleSchemaDownload}
							type="button"
						>
							{Liferay.Language.get('download-schema')}
						</button>

						<button
							className="btn btn-primary"
							data-testid="workflow-json-validate"
							disabled={!canValidate || isBusy}
							onClick={() => _handleWorkflowAction('validate', planResourceURL)}
							type="button"
						>
							{Liferay.Language.get('validate-json')}
						</button>

						<button
							className="btn btn-secondary"
							data-testid="workflow-json-plan"
							disabled={!canValidate || isBusy}
							onClick={() => _handleWorkflowAction('plan', planResourceURL)}
							type="button"
						>
							{Liferay.Language.get('plan-json')}
						</button>

						<button
							className="btn btn-success"
							data-testid="workflow-json-execute"
							disabled={!canExecute || isBusy}
							onClick={() =>
								_handleWorkflowAction('execute', executeResourceURL)
							}
							type="button"
						>
							{Liferay.Language.get('execute-json')}
						</button>
					</div>

					<p className="workflow-json-intro">
						{Liferay.Language.get('workflow-json-help-text')}
					</p>

					<label className="workflow-json-label" htmlFor="workflow-json-editor">
						{Liferay.Language.get('workflow-json-editor')}
					</label>

					<textarea
						className={`workflow-json-textarea ${
							validationError ? 'is-invalid' : ''
						}`}
						data-testid="workflow-json-textarea"
						id="workflow-json-editor"
						onChange={(event) => _applyValue(event.target.value)}
						spellCheck={false}
						value={draftValue}
					/>

					{validationError && (
						<div
							className="workflow-json-inline-error"
							data-testid="workflow-json-editor-error"
						>
							{validationError}
						</div>
					)}

					<div className="workflow-json-helper-note">
						{Liferay.Language.get('workflow-json-action-help')}
					</div>

					{result && (
						<section
							className={`alert alert-${
								result.tone === 'danger'
									? 'danger'
									: result.tone === 'warning'
										? 'warning'
										: result.tone === 'info'
											? 'info'
											: 'success'
							} workflow-json-result-panel`}
							data-testid="workflow-json-result-panel"
						>
							<div className="workflow-json-panel-header">
								<div>
									<p className="workflow-json-eyebrow">
										{Liferay.Language.get('result')}
									</p>
									<h4 data-testid="workflow-json-result-title">
										{result.title}
									</h4>
									<p
										className="workflow-json-result-summary"
										data-testid="workflow-json-result-summary"
									>
										{result.summary}
									</p>
								</div>

								<div className="workflow-json-action-row">
									<span className="workflow-json-result-action">
										{_getActionLabel(result.action)}
									</span>

									<button
										className="btn btn-sm btn-outline-secondary"
										data-testid="workflow-json-result-toggle-details"
										onClick={() =>
											setResultDetailsVisible(
												(currentValue) => !currentValue
											)
										}
										type="button"
									>
										{resultDetailsVisible ? 'Hide details' : 'Show details'}
									</button>
								</div>
							</div>

							{resultDetailsVisible && (
								<pre
									className="workflow-json-result-body"
									data-testid="workflow-json-result-body"
								>
									{result.body}
								</pre>
							)}
						</section>
					)}
				</div>
			</div>
		</section>
	);
}

export default WorkflowJsonEditor;
