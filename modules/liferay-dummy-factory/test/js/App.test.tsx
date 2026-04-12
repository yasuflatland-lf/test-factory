import {describe, expect, it} from 'vitest';

describe('App i18n keys', () => {
	it('resolves liferay-dummy-factory key', () => {
		const text = Liferay.Language.get('liferay-dummy-factory');

		expect(text).not.toBe('liferay-dummy-factory');
		expect(text).toBe('Liferay Dummy Factory');
	});
});
