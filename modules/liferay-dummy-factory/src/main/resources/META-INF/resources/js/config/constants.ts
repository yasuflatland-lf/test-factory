export const ENTITY_TYPES = {
	ORGANIZATION: 'ORG',
	WCM: 'WCM',
	PAGES: 'PAGES',
	DOCUMENTS: 'DOC',
	ROLES: 'ROLES',
	SITES: 'SITES',
	USERS: 'USERS',
	VOCABULARY: 'VOCABULARY',
	COMPANY: 'COMPANY',
	BLOGS: 'BLOGS',
	CATEGORY: 'CATEGORY',
	MB_CATEGORY: 'MB_CATEGORY',
	MB_REPLY: 'MB_REPLY',
	MB_THREAD: 'MB_THREAD',
} as const;

export type EntityType = typeof ENTITY_TYPES[keyof typeof ENTITY_TYPES];

export const ENTITY_LABELS: Record<EntityType, string> = {
	BLOGS: 'blogs',
	CATEGORY: 'categories',
	COMPANY: 'company',
	DOC: 'documents',
	MB_CATEGORY: 'mb-categories',
	MB_REPLY: 'mb-replies',
	MB_THREAD: 'mb-threads',
	ORG: 'organizations',
	PAGES: 'pages',
	ROLES: 'roles',
	SITES: 'sites',
	USERS: 'users',
	VOCABULARY: 'vocabularies',
	WCM: 'web-content',
};

export const ENTITY_ICONS: Record<EntityType, string> = {
	BLOGS: 'blogs',
	CATEGORY: 'categories',
	COMPANY: 'briefcase',
	DOC: 'documents-and-media',
	MB_CATEGORY: 'folder',
	MB_REPLY: 'reply',
	MB_THREAD: 'message',
	ORG: 'organizations',
	PAGES: 'page',
	ROLES: 'roles',
	SITES: 'sites',
	USERS: 'user',
	VOCABULARY: 'categories',
	WCM: 'web-content',
};
