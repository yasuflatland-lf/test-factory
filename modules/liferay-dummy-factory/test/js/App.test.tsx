import {describe, expect, it} from 'vitest';

describe('App breadcrumb i18n keys', () => {
	it('resolves control-panel key', () => {
		const text = Liferay.Language.get('control-panel');

		expect(text).not.toBe('control-panel');
		expect(text).toBe('Control Panel');
	});

	it('resolves configuration key', () => {
		const text = Liferay.Language.get('configuration');

		expect(text).not.toBe('configuration');
		expect(text).toBe('Configuration');
	});

	it('resolves liferay-dummy-factory key', () => {
		const text = Liferay.Language.get('liferay-dummy-factory');

		expect(text).not.toBe('liferay-dummy-factory');
		expect(text).toBe('Liferay Dummy Factory');
	});
});
