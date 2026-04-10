interface ProgressBarProps {
	percent: number;
	running: boolean;
}

function ProgressBar({percent, running}: ProgressBarProps) {
	if (!running && percent === 0) {
		return null;
	}

	return (
		<div className="sheet-section">
			<div className="progress">
				<div
					className="progress-bar"
					role="progressbar"
					style={{width: `${percent}%`}}
				>
					{percent > 0 && `${Math.round(percent)}%`}
				</div>
			</div>
		</div>
	);
}

export default ProgressBar;
