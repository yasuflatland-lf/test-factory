import {useEffect} from 'react';

interface PerSiteResult {
	created: number;
	error?: string;
	failed: number;
	groupId: number;
	siteName: string;
}

interface MultiSiteResult {
	ok: boolean;
	perSite: PerSiteResult[];
	totalCreated: number;
	totalRequested: number;
}

interface ResultAlertProps {
	message: string | null;
	multiSite?: MultiSiteResult | null;
	onDismiss: () => void;
	type: 'success' | 'danger';
}

function ResultAlert({message, multiSite, onDismiss, type}: ResultAlertProps) {
	const effectiveType: 'success' | 'danger' | 'warning' = multiSite
		? multiSite.ok
			? 'success'
			: multiSite.totalCreated > 0
				? 'warning'
				: 'danger'
		: type;

	const hasContent = Boolean(message) || Boolean(multiSite);

	useEffect(() => {
		if (hasContent && effectiveType === 'success') {
			const timer = setTimeout(onDismiss, 5000);

			return () => clearTimeout(timer);
		}
	}, [effectiveType, hasContent, onDismiss]);

	if (!hasContent) {
		return null;
	}

	return (
		<div
			className={`alert alert-${effectiveType} alert-dismissible`}
			role="alert"
		>
			<button
				className="close"
				onClick={onDismiss}
				type="button"
			>
				<span aria-hidden="true">&times;</span>
			</button>

			{multiSite ? (
				<>
					<div>
						{Liferay.Language.get('per-site-results')}{' '}
						{multiSite.totalCreated}/{multiSite.totalRequested}
					</div>

					<ul className="list-unstyled">
						{multiSite.perSite.map((entry) => (
							<li key={entry.groupId}>
								{entry.siteName}: {entry.created}/
								{entry.created + entry.failed}
								{entry.error ? ` — ${entry.error}` : ''}
							</li>
						))}
					</ul>
				</>
			) : (
				message
			)}
		</div>
	);
}

export default ResultAlert;
