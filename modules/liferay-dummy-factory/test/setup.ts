(global as any).Liferay = {
	authToken: 'test-auth-token',
	Language: {
		get(key: string): string {
			return key;
		},
	},
};
