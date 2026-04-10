import {useEffect} from 'react';

interface ResultAlertProps {
	message: string | null;
	onDismiss: () => void;
	type: 'success' | 'danger';
}

function ResultAlert({message, onDismiss, type}: ResultAlertProps) {
	useEffect(() => {
		if (message && type === 'success') {
			const timer = setTimeout(onDismiss, 5000);

			return () => clearTimeout(timer);
		}
	}, [message, onDismiss, type]);

	if (!message) {
		return null;
	}

	return (
		<div className={`alert alert-${type} alert-dismissible`} role="alert">
			<button
				className="close"
				onClick={onDismiss}
				type="button"
			>
				<span aria-hidden="true">&times;</span>
			</button>

			{message}
		</div>
	);
}

export default ResultAlert;
