create table TestFactory_CalcEntry (
	calcEntryId LONG not null primary key,
	companyId LONG,
	userId LONG,
	userName VARCHAR(75) null,
	createDate DATE null,
	modifiedDate DATE null,
	num1 DOUBLE,
	num2 DOUBLE,
	operator VARCHAR(75) null,
	result DOUBLE
);