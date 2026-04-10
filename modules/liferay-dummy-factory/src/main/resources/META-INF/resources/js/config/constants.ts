export const ENTITY_TYPES = {
	BLOGS: 'BLOGS',
	CATEGORY: 'CATEGORY',
	COMPANY: 'COMPANY',
	DOCUMENTS: 'DOC',
	MB: 'MB',
	ORGANIZATION: 'ORG',
	PAGES: 'PAGES',
	SITES: 'SITES',
	USERS: 'USERS',
	WCM: 'WCM',
} as const;

export type EntityType = typeof ENTITY_TYPES[keyof typeof ENTITY_TYPES];

export const ENTITY_LABELS: Record<EntityType, string> = {
	BLOGS: 'blogs',
	CATEGORY: 'categories',
	COMPANY: 'company',
	DOC: 'documents',
	MB: 'message-boards',
	ORG: 'organizations',
	PAGES: 'pages',
	SITES: 'sites',
	USERS: 'users',
	WCM: 'web-content',
};

export const ENTITY_ICONS: Record<EntityType, string> = {
	BLOGS: 'blogs',
	CATEGORY: 'categories',
	COMPANY: 'briefcase',
	DOC: 'documents-and-media',
	MB: 'message-boards',
	ORG: 'organizations',
	PAGES: 'page',
	SITES: 'sites',
	USERS: 'user',
	WCM: 'web-content',
};
