import {ReactNode, useState} from 'react';

interface AdvancedOptionsProps {
	children: ReactNode;
}

function AdvancedOptions({children}: AdvancedOptionsProps) {
	const [expanded, setExpanded] = useState(false);

	return (
		<div className="sheet-section">
			<button
				className="btn btn-link btn-sm"
				onClick={() => setExpanded(!expanded)}
				type="button"
			>
				{Liferay.Language.get('advanced-options')}

				<span className={`ml-2 icon-${expanded ? 'minus' : 'plus'}`} />
			</button>

			{expanded && <div className="mt-3">{children}</div>}
		</div>
	);
}

export default AdvancedOptions;
