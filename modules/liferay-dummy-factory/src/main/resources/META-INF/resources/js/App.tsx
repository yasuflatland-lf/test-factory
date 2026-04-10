import {useState} from 'react';

import {ENTITY_TYPES, EntityType} from './config/constants';
import {getEntityConfig} from './config/entities';
import EntityForm from './components/EntityForm';
import EntitySelector from './components/EntitySelector';

interface AppProps {
	actionResourceURL: string;
	dataResourceURL: string;
	progressResourceURL: string;
}

function App({actionResourceURL, dataResourceURL, progressResourceURL}: AppProps) {
	const [selectedEntity, setSelectedEntity] = useState<EntityType>(
		ENTITY_TYPES.ORGANIZATION
	);

	const entityConfig = getEntityConfig(selectedEntity);

	return (
		<div className="container-fluid">
			<div className="row">
				<div className="col-md-2">
					<EntitySelector
						onSelect={setSelectedEntity}
						selected={selectedEntity}
					/>
				</div>

				<div className="col-md-10">
					{entityConfig ? (
						<EntityForm
							actionResourceURL={actionResourceURL}
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
