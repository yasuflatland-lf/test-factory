import {ENTITY_ICONS, ENTITY_LABELS, ENTITY_TYPES, EntityType} from '../config/constants';

interface EntitySelectorProps {
	onSelect: (entityType: EntityType) => void;
	selected: EntityType;
}

const ENTITY_LIST = Object.values(ENTITY_TYPES);

function EntitySelector({onSelect, selected}: EntitySelectorProps) {
	return (
		<div className="card-page card-page-equal-height">
			{ENTITY_LIST.map((entityType) => (
				<div className="card-page-item col-md-3 col-sm-6" key={entityType}>
					<div
						className={`card card-interactive card-interactive-primary ${
							selected === entityType ? 'active' : ''
						}`}
						onClick={() => onSelect(entityType)}
						role="button"
						tabIndex={0}
					>
						<div className="card-body">
							<div className="card-row">
								<div className="autofit-col">
									<span className="sticker sticker-primary">
										<svg className="lexicon-icon">
											<use
												xlinkHref={`${Liferay.ThemeDisplay.getPathThemeImages()}/clay/icons.svg#${ENTITY_ICONS[entityType]}`}
											/>
										</svg>
									</span>
								</div>

								<div className="autofit-col autofit-col-expand">
									<div className="card-title">
										{Liferay.Language.get(ENTITY_LABELS[entityType])}
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			))}
		</div>
	);
}

export default EntitySelector;
