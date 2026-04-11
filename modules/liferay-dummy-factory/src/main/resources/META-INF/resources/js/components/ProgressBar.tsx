interface ProgressBarProps {
	percent: number;
	running: boolean;
	testId?: string;
}

function ProgressBar({percent, running, testId}: ProgressBarProps) {
	if (!running && percent === 0) {
		return null;
	}

	return (
		<div className="sheet-section">
			<div
				className="progress"
				data-testid={testId}
			>
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
