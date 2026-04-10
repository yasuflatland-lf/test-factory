const path = require('path');

const localReact = path.resolve(__dirname, 'node_modules/react');

module.exports = {
	modulePathIgnorePatterns: ['<rootDir>/classes/'],
	moduleNameMapper: {
		'^react$': localReact,
		'^react/(.*)$': localReact + '/$1',
	},
	setupFiles: ['./test/setup.ts'],
	testEnvironment: 'jsdom',
	testMatch: ['<rootDir>/test/**/*.test.{ts,tsx}'],
	transform: {
		'^.+\\.tsx?$': 'babel-jest',
	},
};
