import * as React from 'react';

(global as any).React = React;

(global as any).Liferay = {
	authToken: 'test-auth-token',
	Language: {
		get(key: string): string {
			return key;
		},
	},
};
