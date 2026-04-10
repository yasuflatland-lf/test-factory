declare const Liferay: {
	ThemeDisplay: {
		getCompanyId(): string;
		getScopeGroupId(): string;
	};
	authToken: string;
	Language: {
		get(key: string): string;
	};
};
