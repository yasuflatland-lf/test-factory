import {useState} from 'react';

interface CalculatorProps {
	calculateURL: string;
}

function Calculator({calculateURL}: CalculatorProps) {
	const [num1, setNum1] = useState<string>('');
	const [num2, setNum2] = useState<string>('');
	const [operator, setOperator] = useState<string>('+');
	const [result, setResult] = useState<number | null>(null);
	const [error, setError] = useState<string | null>(null);

	const [loading, setLoading] = useState<boolean>(false);

	const handleCalculate = () => {
		setLoading(true);
		setError(null);

		const url =
			calculateURL +
			'&num1=' + encodeURIComponent(num1) +
			'&num2=' + encodeURIComponent(num2) +
			'&operator=' + encodeURIComponent(operator);

		fetch(url, {
			credentials: 'include',
			method: 'GET',
		})
			.then((response) => response.json())
			.then((data) => {
				if (data.error) {
					setError(data.error);
				}
				else {
					setResult(data.result);
				}
			})
			.catch((err) => {
				setError(err.message);
			})
			.finally(() => {
				setLoading(false);
			});
	};

	return (
		<div className="container-fluid container-fluid-max-xl">
			<div className="sheet sheet-lg">
				<div className="sheet-header">
					<h2>{Liferay.Language.get('calculator')}</h2>
				</div>

				<div className="sheet-section">
					<div className="form-group">
						<label htmlFor="num1">
							{Liferay.Language.get('number-1')}
						</label>

						<input
							className="form-control"
							id="num1"
							onChange={(e) => setNum1(e.target.value)}
							type="number"
							value={num1}
						/>
					</div>

					<div className="form-group">
						<label htmlFor="operator">
							{Liferay.Language.get('operator')}
						</label>

						<select
							className="form-control"
							id="operator"
							onChange={(e) =>
								setOperator(e.target.value)
							}
							value={operator}
						>
							<option value="+">+</option>
							<option value="-">-</option>
							<option value="*">&times;</option>
							<option value="/">&divide;</option>
						</select>
					</div>

					<div className="form-group">
						<label htmlFor="num2">
							{Liferay.Language.get('number-2')}
						</label>

						<input
							className="form-control"
							id="num2"
							onChange={(e) => setNum2(e.target.value)}
							type="number"
							value={num2}
						/>
					</div>

					<button
						className="btn btn-primary"
						disabled={loading}
						onClick={handleCalculate}
						type="button"
					>
						{loading
							? Liferay.Language.get('calculating')
							: Liferay.Language.get('calculate')}
					</button>
				</div>

				{result !== null && (
					<div className="sheet-footer">
						<div className="alert alert-success">
							{Liferay.Language.get('result')}: {result}
						</div>
					</div>
				)}

				{error && (
					<div className="sheet-footer">
						<div className="alert alert-danger">{error}</div>
					</div>
				)}
			</div>
		</div>
	);
}

export default Calculator;
