import {ENTITY_LABELS, ENTITY_TYPES, EntityType} from '../config/constants';

interface EntitySelectorProps {
	onSelect: (entityType: EntityType) => void;
	selected: EntityType;
}

const ENTITY_LIST = Object.values(ENTITY_TYPES);

function EntitySelector({onSelect, selected}: EntitySelectorProps) {
	return (
		<nav className="menubar menubar-transparent menubar-vertical-expand-md">
			<ul className="nav nav-nested">
				{ENTITY_LIST.map((entityType) => (
					<li className="nav-item" key={entityType}>
						<button
							className={`btn btn-unstyled nav-link ${
								selected === entityType ? 'active' : ''
							}`}
							onClick={() => onSelect(entityType)}
							type="button"
						>
							{Liferay.Language.get(ENTITY_LABELS[entityType])}
						</button>
					</li>
				))}
			</ul>
		</nav>
	);
}

export default EntitySelector;
