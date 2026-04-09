import React, {useState} from 'react';

function Calculator() {
	const [num1, setNum1] = useState('');
	const [num2, setNum2] = useState('');
	const [operator, setOperator] = useState('+');
	const [result, setResult] = useState(null);
	const [error, setError] = useState(null);
	const [loading, setLoading] = useState(false);

	const handleCalculate = () => {
		setLoading(true);
		setError(null);

		const formData = new FormData();

		formData.append('num1', num1);
		formData.append('num2', num2);
		formData.append('operator', operator);
		formData.append('serviceContext', JSON.stringify({}));

		fetch('/api/jsonws/TestFactory.CalcEntry/calculate', {
			body: formData,
			credentials: 'include',
			headers: {
				'x-csrf-token': Liferay.authToken,
			},
			method: 'POST',
		})
			.then((response) => response.json())
			.then((data) => {
				if (data.exception) {
					setError(data.exception);
				}
				else {
					setResult(data.result);
				}

				setLoading(false);
			})
			.catch((err) => {
				setError(err.message);
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
