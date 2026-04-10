declare const Liferay: {
	Language: {
		get(key: string): string;
	};
	ThemeDisplay: {
		getCompanyId(): string;
		getPathThemeImages(): string;
		getScopeGroupId(): string;
	};
	authToken: string;
};
