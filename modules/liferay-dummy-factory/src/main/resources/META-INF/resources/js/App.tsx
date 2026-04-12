import {useState} from 'react';

import {ENTITY_TYPES, EntityType} from './config/constants';
import {getEntityConfig} from './config/entities';
import EntityForm from './components/EntityForm';
import EntitySelector from './components/EntitySelector';

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

	return (
		<div className="container-fluid container-fluid-max-xl">
			<ol className="breadcrumb">
				<li className="breadcrumb-item">
					<span className="text-truncate">
						{Liferay.Language.get('control-panel')}
					</span>
				</li>
				<li className="breadcrumb-item">
					<span className="text-truncate">
						{Liferay.Language.get('configuration')}
					</span>
				</li>
				<li className="breadcrumb-item active">
					<span className="text-truncate">
						{Liferay.Language.get('liferay-dummy-factory')}
					</span>
				</li>
			</ol>
			<div className="row">
				<div className="col-md-2 pr-0">
					<EntitySelector
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
		</div>
	);
}

export default App;
