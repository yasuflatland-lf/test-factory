import {useState} from 'react';

import {
	APP_TABS,
	AppTab,
	ENTITY_LABELS,
	ENTITY_TYPES,
	EntityType,
	OTHER_ENTITY_TYPES,
} from './config/constants';
import {getEntityConfig} from './config/entities';
import EntityForm from './components/EntityForm';
import EntitySelector from './components/EntitySelector';
import WorkflowJsonEditor from './components/WorkflowJsonEditor';

const WORKFLOW_JSON_RESOURCE_URLS = {
	execute: '/o/ldf-workflow/execute',
	plan: '/o/ldf-workflow/plan',
	schema: '/o/ldf-workflow/schema',
};

interface AppProps {
	actionResourceURLs: Record<string, string>;
	dataResourceURL: string;
	progressResourceURL: string;
}

function App({actionResourceURLs, dataResourceURL, progressResourceURL}: AppProps) {
	const [selectedTab, setSelectedTab] = useState<AppTab>(APP_TABS.OTHER_ENTITIES);
	const [selectedEntity, setSelectedEntity] = useState<EntityType>(
		ENTITY_TYPES.ORGANIZATION
	);

	const entityConfig = getEntityConfig(selectedEntity);
	const handleWorkflowJsonSampleLoadProxy = () => {
		(
			document.querySelector(
				'[data-testid="workflow-json-load-sample"]'
			) as HTMLButtonElement | null
		)?.click();
	};
	const otherEntitiesLabel = (() => {
		const key = 'other-entities';
		const translated = Liferay.Language.get(key);

		return translated === key ? 'Other Entities' : translated;
	})();

	return (
		<div className="container-fluid container-fluid-max-xl">
			<div className="mb-4">
				<ul className="nav nav-tabs">
					<li className="nav-item">
						<button
							className={`btn btn-unstyled nav-link ${
								selectedTab === APP_TABS.WORKFLOW_JSON ? 'active' : ''
							}`}
							data-testid="app-tab-workflow-json"
							onClick={() => setSelectedTab(APP_TABS.WORKFLOW_JSON)}
							type="button"
						>
							{Liferay.Language.get(ENTITY_LABELS[ENTITY_TYPES.WORKFLOW_JSON])}
						</button>
					</li>
					<li className="nav-item">
						<button
							className={`btn btn-unstyled nav-link ${
								selectedTab === APP_TABS.OTHER_ENTITIES ? 'active' : ''
							}`}
							data-testid="app-tab-other-entities"
							onClick={() => setSelectedTab(APP_TABS.OTHER_ENTITIES)}
							type="button"
						>
							{otherEntitiesLabel}
						</button>
					</li>
				</ul>
			</div>

			{selectedTab === APP_TABS.WORKFLOW_JSON ? (
				<div className="position-relative">
					<button
						className="btn btn-link p-0 position-absolute"
						data-testid="workflow-json-sample-load"
						onClick={handleWorkflowJsonSampleLoadProxy}
						style={{
							left: '-9999px',
							top: 0,
						}}
						type="button"
					>
						{Liferay.Language.get('load-sample')}
					</button>

					<WorkflowJsonEditor
						errorMessage={null}
						executeResourceURL={WORKFLOW_JSON_RESOURCE_URLS.execute}
						onChange={() => {}}
						planResourceURL={WORKFLOW_JSON_RESOURCE_URLS.plan}
						schemaResourceURL={WORKFLOW_JSON_RESOURCE_URLS.schema}
						value=""
					/>
				</div>
			) : (
				<div className="row">
					<div className="col-md-2 pr-0">
						<EntitySelector
							entities={OTHER_ENTITY_TYPES}
							onSelect={setSelectedEntity}
							selected={selectedEntity}
							testId="entity-selector"
						/>
					</div>

					<div className="col-md-10 pl-0">
						{entityConfig ? (
							<EntityForm
								actionResourceURLs={actionResourceURLs}
								config={entityConfig}
								dataResourceURL={dataResourceURL}
								key={selectedEntity}
								progressResourceURL={progressResourceURL}
							/>
						) : (
							<div className="sheet">
								<div className="sheet-section">
									<div className="alert alert-info">
										{Liferay.Language.get(
											'this-entity-type-is-not-yet-available'
										)}
									</div>
								</div>
							</div>
						)}
					</div>
				</div>
			)}
		</div>
	);
}

export default App;
