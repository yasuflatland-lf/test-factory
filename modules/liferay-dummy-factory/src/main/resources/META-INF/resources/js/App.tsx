import {useState} from 'react';

import {ENTITY_TYPES, EntityType} from './config/constants';
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
	const [selectedEntity, setSelectedEntity] = useState<EntityType>(
		ENTITY_TYPES.ORGANIZATION
	);

	const entityConfig = getEntityConfig(selectedEntity);
	const isWorkflowJson = selectedEntity === ENTITY_TYPES.WORKFLOW_JSON;
	const handleWorkflowJsonSampleLoadProxy = () => {
		(
			document.querySelector(
				'[data-testid="workflow-json-load-sample"]'
			) as HTMLButtonElement | null
		)?.click();
	};

	return (
		<div className="container-fluid container-fluid-max-xl">
			<div className="row">
				<div className="col-md-2 pr-0">
					<EntitySelector
						onSelect={setSelectedEntity}
						selected={selectedEntity}
						testId="entity-selector"
					/>
				</div>

				<div className="col-md-10 pl-0">
					{isWorkflowJson ? (
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
					) : entityConfig ? (
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
		</div>
	);
}

export default App;
