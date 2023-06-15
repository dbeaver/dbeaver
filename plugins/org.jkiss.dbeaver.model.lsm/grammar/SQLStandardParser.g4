/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
parser grammar SQLStandardParser;

options {
    tokenVocab=SQLStandardLexer;
    superClass=org.jkiss.dbeaver.model.stm.STMParserOverrides;
    contextSuperClass=org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
}

@header {
    /*
     * DBeaver - Universal Database Manager
     * Copyright (C) 2010-2023 DBeaver Corp and others
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    package org.jkiss.dbeaver.model.lsm.sql.impl.syntax;
}

@members {
private boolean isSupportSquareBracketQuotation;
public boolean isSupportSquareBracketQuotation() { return isSupportSquareBracketQuotation; }
public void setIsSupportSquareBracketQuotation(boolean value) { isSupportSquareBracketQuotation = value; }
}


// identifiers
characterSetSpecification: characterSetName;
characterSetName: (schemaName Period)? Identifier;
schemaName: (catalogName Period)? unqualifiedSchemaName;
unqualifiedSchemaName: identifier;
catalogName: identifier;
identifier: (Introducer characterSetSpecification)? actualIdentifier;
actualIdentifier: (Identifier|DelimitedIdentifier|squareBracketIdentifier|nonReserved);
squareBracketIdentifier: {isSupportSquareBracketQuotation()}? '[' (~']' | ']' ']')* ']';

// date-time literals
dateString: SingleQuote dateValue SingleQuote;
dateValue: yearsValue MinusSign monthsValue MinusSign daysValue;
yearsValue: datetimeValue;
datetimeValue: UnsignedInteger;
monthsValue: datetimeValue;
daysValue: datetimeValue;
timeString: SingleQuote timeValue (timeZoneInterval)? SingleQuote;
timeValue: hoursValue Colon minutesValue Colon secondsValue;
hoursValue: datetimeValue;
minutesValue: datetimeValue;
secondsValue: secondsIntegerValue (Period (secondsFraction)?)?;
secondsIntegerValue: UnsignedInteger;
secondsFraction: UnsignedInteger;
timeZoneInterval: Sign hoursValue Colon minutesValue;
timestampString: SingleQuote dateValue Space timeValue (timeZoneInterval)? SingleQuote;
intervalString: SingleQuote (yearMonthLiteral|dayTimeLiteral) SingleQuote;
yearMonthLiteral: (yearsValue|(yearsValue MinusSign)? monthsValue);
dayTimeLiteral: (dayTimeInterval|timeInterval);
dayTimeInterval: daysValue (Space hoursValue (Colon minutesValue (Colon secondsValue)?)?)?;
timeInterval: (hoursValue (Colon minutesValue (Colon secondsValue)?)?|minutesValue (Colon secondsValue)?|secondsValue);

// module declaration - base rules
module: moduleNameClause languageClause moduleAuthorizationClause ((temporaryTableDeclaration)+)? (moduleContents)+;
moduleNameClause: MODULE (moduleName)? (moduleCharacterSetSpecification)?;
moduleName: identifier;
moduleCharacterSetSpecification: NAMES ARE characterSetSpecification;
languageClause: LANGUAGE languageName;
languageName: (ADA|C_|COBOL|FORTRAN|MUMPS|PASCAL|PLI);
moduleAuthorizationClause: (SCHEMA schemaName|AUTHORIZATION moduleAuthorizationIdentifier|SCHEMA schemaName AUTHORIZATION moduleAuthorizationIdentifier);
moduleAuthorizationIdentifier: authorizationIdentifier;
authorizationIdentifier: identifier;

// data types
dataType: (characterStringType (CHARACTER SET characterSetSpecification)?|nationalCharacterStringType|bitStringType|numericType|datetimeType|intervalType);
characterStringType: (CHARACTER (LeftParen length RightParen)?|CHAR (LeftParen length RightParen)?|CHARACTER VARYING (LeftParen length RightParen)?|CHAR VARYING (LeftParen length RightParen)?|VARCHAR (LeftParen length RightParen)?);
length: UnsignedInteger;
nationalCharacterStringType: (NATIONAL CHARACTER (LeftParen length RightParen)?|NATIONAL CHAR (LeftParen length RightParen)?|NCHAR (LeftParen length RightParen)?|NATIONAL CHARACTER VARYING (LeftParen length RightParen)?|NATIONAL CHAR VARYING (LeftParen length RightParen)?|NCHAR VARYING (LeftParen length RightParen)?);
bitStringType: (BIT (LeftParen length RightParen)?|BIT VARYING (LeftParen length RightParen)?);
numericType: (exactNumericType|approximateNumericType);
exactNumericType: (NUMERIC (LeftParen precision (Comma scale)? RightParen)?|DECIMAL (LeftParen precision (Comma scale)? RightParen)?|DEC (LeftParen precision (Comma scale)? RightParen)?|INTEGER|INT|SMALLINT);
precision: UnsignedInteger;
scale: UnsignedInteger;
approximateNumericType: (FLOAT (LeftParen precision RightParen)?|REAL|DOUBLE PRECISION);
datetimeType: (DATE|TIME (LeftParen timePrecision RightParen)? (WITH TIME ZONE)?|TIMESTAMP (LeftParen timestampPrecision RightParen)? (WITH TIME ZONE)?);
timePrecision: timeFractionalSecondsPrecision;
timeFractionalSecondsPrecision: UnsignedInteger;
timestampPrecision: timeFractionalSecondsPrecision;
intervalType: INTERVAL intervalQualifier;
intervalQualifier: (startField TO endField|singleDatetimeField);
startField: nonSecondDatetimeField (LeftParen intervalLeadingFieldPrecision RightParen)?;
nonSecondDatetimeField: (YEAR|MONTH|DAY|HOUR|MINUTE);
intervalLeadingFieldPrecision: UnsignedInteger;
endField: (nonSecondDatetimeField|SECOND (LeftParen intervalFractionalSecondsPrecision RightParen)?);
intervalFractionalSecondsPrecision: UnsignedInteger;
singleDatetimeField: (nonSecondDatetimeField (LeftParen intervalLeadingFieldPrecision RightParen)?|SECOND (LeftParen intervalLeadingFieldPrecision (Comma intervalFractionalSecondsPrecision)? RightParen)?);

// table definition
temporaryTableDeclaration: DECLARE LOCAL TEMPORARY TABLE qualifiedLocalTableName tableElementList (ON COMMIT (PRESERVE|DELETE) ROWS)?;
qualifiedLocalTableName: MODULE Period localTableName;
localTableName: qualifiedIdentifier;
qualifiedIdentifier: identifier;
tableElementList: LeftParen tableElement ((Comma tableElement)+)? RightParen;
tableElement: (columnDefinition|tableConstraintDefinition);

// column definition
columnDefinition: columnName (dataType|domainName) (defaultClause)? ((columnConstraintDefinition)+)? (collateClause)?;
columnName: identifier;

// domain
domainName: qualifiedName;
qualifiedName: (schemaName Period)? qualifiedIdentifier;

// default
defaultClause: DEFAULT defaultOption;
defaultOption: (literal|datetimeValueFunction|USER|CURRENT_USER|SESSION_USER|SYSTEM_USER|NULL);

// data type literals
literal: (signedNumericLiteral|generalLiteral);
unsignedNumericLiteral: (UnsignedInteger|DecimalLiteral|ApproximateNumericLiteral);
signedNumericLiteral: (Sign)? unsignedNumericLiteral;
characterStringLiteral: (Introducer characterSetSpecification)? StringLiteralContent;
generalLiteral: (characterStringLiteral|NationalCharacterStringLiteral|BitStringLiteral|HexStringLiteral|datetimeLiteral|intervalLiteral);
datetimeLiteral: (dateLiteral|timeLiteral|timestampLiteral);
dateLiteral: DATE dateString;
timeLiteral: TIME timeString;
timestampLiteral: TIMESTAMP timestampString;
intervalLiteral: INTERVAL (Sign)? intervalString intervalQualifier;
datetimeValueFunction: (currentDateValueFunction|currentTimeValueFunction|currentTimestampValueFunction);
currentDateValueFunction: CURRENT_DATE;
currentTimeValueFunction: CURRENT_TIME (LeftParen timePrecision RightParen)?;
currentTimestampValueFunction: CURRENT_TIMESTAMP (LeftParen timestampPrecision RightParen)?;

// column constraints
columnConstraintDefinition: (constraintNameDefinition)? columnConstraint (constraintAttributes)?;
constraintNameDefinition: CONSTRAINT constraintName;
constraintName: qualifiedName;
columnConstraint: (NOT NULL|uniqueSpecification|referencesSpecification|checkConstraintDefinition);
uniqueSpecification: (UNIQUE|PRIMARY KEY);
referencesSpecification: REFERENCES referencedTableAndColumns (MATCH matchType)? (referentialTriggeredAction)?;
referencedTableAndColumns: tableName (LeftParen referenceColumnList RightParen)?;
tableName: (qualifiedName|qualifiedLocalTableName);
referenceColumnList: columnNameList;
columnNameList: columnName ((Comma columnName)+)?;
matchType: (FULL|PARTIAL);
referentialTriggeredAction: (updateRule (deleteRule)?|deleteRule (updateRule)?);
updateRule: ON UPDATE referentialAction;
referentialAction: (CASCADE|SET NULL|SET DEFAULT|NO ACTION);
deleteRule: ON DELETE referentialAction;
checkConstraintDefinition: CHECK LeftParen searchCondition RightParen;
searchCondition: (booleanTerm|(.*?)) (OR booleanTerm)*; // (.*?) - for error recovery
booleanTerm: booleanFactor (AND booleanFactor)*;
booleanFactor: (NOT)? booleanTest;
booleanTest: booleanPrimary (IS (NOT)? truthValue)?;
booleanPrimary: (predicate|LeftParen searchCondition RightParen);
predicate: (comparisonPredicate|betweenPredicate|inPredicate|likePredicate|nullPredicate|quantifiedComparisonPredicate|existsPredicate|matchPredicate|overlapsPredicate);
comparisonPredicate: rowValueConstructor compOp rowValueConstructor;
rowValueConstructor: (rowValueConstructorElement|LeftParen rowValueConstructorList RightParen|rowSubquery);
rowValueConstructorElement: (valueExpression|nullSpecification|defaultSpecification);
valueExpression: (numericValueExpression|stringValueExpression|datetimeValueExpression|intervalValueExpression);
numericValueExpression: term ((PlusSign term)|(MinusSign term))*;
term: factor ((Asterisk factor)|(Solidus factor))*;
factor: (Sign)? numericPrimary;
numericPrimary: (valueExpressionPrimary|numericValueFunction);
valueExpressionPrimary: (unsignedValueSpecification|columnReference|setFunctionSpecification|scalarSubquery|caseExpression|LeftParen valueExpression RightParen|castSpecification);
unsignedValueSpecification: (unsignedLiteral|generalValueSpecification);
unsignedLiteral: (unsignedNumericLiteral|generalLiteral);
generalValueSpecification: (parameterSpecification|dynamicParameterSpecification|USER|CURRENT_USER|SESSION_USER|SYSTEM_USER|VALUE);
parameterSpecification: parameterName (indicatorParameter)?;
parameterName: Colon identifier;
indicatorParameter: (INDICATOR)? parameterName;
dynamicParameterSpecification: QuestionMark;
columnReference: (qualifier Period)? columnName;
qualifier: (tableName|correlationName);
correlationName: identifier;
setFunctionSpecification: (COUNT LeftParen Asterisk RightParen|generalSetFunction);
generalSetFunction: setFunctionType LeftParen (setQuantifier)? valueExpression RightParen;
setFunctionType: (AVG|MAX|MIN|SUM|COUNT);
setQuantifier: (DISTINCT|ALL);

// select, subquery
scalarSubquery: subquery;
subquery: LeftParen queryExpression RightParen;
unionTerm: UNION (ALL)? (correspondingSpec)? queryTerm;
exceptTerm: EXCEPT (ALL)? (correspondingSpec)? queryTerm;
nonJoinQueryExpression: nonJoinQueryTerm|queryExpression;
nonJoinQueryTerm: queryPrimary intersectTerm*;
intersectTerm: (INTERSECT (ALL)? (correspondingSpec)? queryPrimary);
nonJoinQueryPrimary: (simpleTable|LeftParen nonJoinQueryExpression RightParen);
simpleTable: (querySpecification|tableValueConstructor|explicitTable);
querySpecification: SELECT (setQuantifier)? selectList tableExpression?;
selectList: (Asterisk|selectSublist) (Comma selectSublist)*; // (Comma selectSublist)* contains any quantifier for error recovery;
selectSublist: (derivedColumn|qualifier Period Asterisk)*; // * for whole rule to handle select fields autocompletion when from immediately after select 
derivedColumn: valueExpression (asClause)?;
asClause: (AS)? columnName;
tableExpression: (.*?) fromClause (whereClause)? (groupByClause)? (havingClause)?; // (.*?) - for error recovery
queryPrimary: (nonJoinQueryPrimary|joinedTable);
queryTerm: (nonJoinQueryTerm|joinedTable);
queryExpression: (joinedTable|nonJoinQueryTerm) (unionTerm|exceptTerm)*;

// from
fromClause: FROM tableReference ((Comma tableReference)+)?;
nonjoinedTableReference: (tableName (correlationSpecification)?)|(derivedTable correlationSpecification);
tableReference: nonjoinedTableReference|joinedTable;
joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
correlationSpecification: (AS)? correlationName (LeftParen derivedColumnList RightParen)?;
derivedColumnList: columnNameList;
derivedTable: tableSubquery;
tableSubquery: subquery;

//joins
crossJoinTerm: CROSS JOIN tableReference;
naturalJoinTerm: (NATURAL)? (joinType)? JOIN tableReference (joinSpecification|(.*?))?; // (.*?) - for error recovery
joinType: (INNER|outerJoinType (OUTER)?|UNION);
outerJoinType: (LEFT|RIGHT|FULL);
joinSpecification: (joinCondition|namedColumnsJoin);
joinCondition: ON searchCondition;
namedColumnsJoin: USING LeftParen joinColumnList RightParen;
joinColumnList: columnNameList;

// conditions
whereClause: WHERE searchCondition;
groupByClause: GROUP BY groupingColumnReferenceList;
groupingColumnReferenceList: groupingColumnReference ((Comma groupingColumnReference)+)?;
groupingColumnReference: columnReference (collateClause)?;
collateClause: COLLATE collationName;
collationName: qualifiedName;
havingClause: HAVING searchCondition;
tableValueConstructor: VALUES tableValueConstructorList;
tableValueConstructorList: rowValueConstructor ((Comma rowValueConstructor)+)?;
explicitTable: TABLE tableName;
correspondingSpec: CORRESPONDING (BY LeftParen correspondingColumnList RightParen)?;
correspondingColumnList: columnNameList;
caseExpression: (caseAbbreviation|caseSpecification);
caseAbbreviation: (NULLIF LeftParen valueExpression Comma valueExpression RightParen|COALESCE LeftParen valueExpression (Comma valueExpression)+ RightParen);
caseSpecification: (simpleCase|searchedCase);
simpleCase: CASE caseOperand (simpleWhenClause)+ (elseClause)? END;
caseOperand: valueExpression;
simpleWhenClause: WHEN whenOperand THEN result;
whenOperand: valueExpression;
result: (resultExpression|NULL);
resultExpression: valueExpression;
elseClause: ELSE result;
searchedCase: CASE (searchedWhenClause)+ (elseClause)? END;
searchedWhenClause: WHEN searchCondition THEN result;
castSpecification: CAST LeftParen castOperand AS castTarget RightParen;
castOperand: (valueExpression|NULL);
castTarget: (domainName|dataType);
numericValueFunction: (positionExpression|extractExpression|lengthExpression);
positionExpression: POSITION LeftParen characterValueExpression IN characterValueExpression RightParen;
characterValueExpression: (concatenation|characterFactor);
concatenation: characterFactor (ConcatenationOperator characterFactor)+;
characterFactor: characterPrimary (collateClause)?;
characterPrimary: (valueExpressionPrimary|stringValueFunction);

// functions and operators
stringValueFunction: (characterValueFunction|bitValueFunction);
characterValueFunction: (characterSubstringFunction|fold|formOfUseConversion|characterTranslation|trimFunction);
characterSubstringFunction: SUBSTRING LeftParen characterValueExpression FROM startPosition (FOR stringLength)? RightParen;
startPosition: numericValueExpression;
stringLength: numericValueExpression;
fold: (UPPER|LOWER) LeftParen characterValueExpression RightParen;
formOfUseConversion: CONVERT LeftParen characterValueExpression USING formOfUseConversionName RightParen;
formOfUseConversionName: qualifiedName;
characterTranslation: TRANSLATE LeftParen characterValueExpression USING translationName RightParen;
translationName: qualifiedName;
trimFunction: TRIM LeftParen trimOperands RightParen;
trimOperands: ((trimSpecification)? (trimCharacter)? FROM)? trimSource;
trimSpecification: (LEADING|TRAILING|BOTH);
trimCharacter: characterValueExpression;
trimSource: characterValueExpression;
bitValueFunction: bitSubstringFunction;
bitSubstringFunction: SUBSTRING LeftParen bitValueExpression FROM startPosition (FOR stringLength)? RightParen;
bitValueExpression: (bitConcatenation|bitFactor);
bitConcatenation: bitFactor (ConcatenationOperator bitFactor)+;
bitFactor: bitPrimary;
bitPrimary: (valueExpressionPrimary|stringValueFunction);
extractExpression: EXTRACT LeftParen extractField FROM extractSource RightParen;
extractField: (datetimeField|timeZoneField);
datetimeField: (nonSecondDatetimeField|SECOND);
timeZoneField: (TIMEZONE_HOUR|TIMEZONE_MINUTE);
extractSource: (datetimeValueExpression|intervalValueExpression);
datetimeValueExpression: (datetimeTerm|(intervalValueExpression PlusSign datetimeTerm)) ((PlusSign intervalTerm)|(MinusSign intervalTerm))*;
intervalTerm: ((intervalFactor)|(term Asterisk intervalFactor)) ((Asterisk factor)|(Solidus factor))*;
intervalFactor: (Sign)? intervalPrimary;
intervalPrimary: valueExpressionPrimary (intervalQualifier)?;
intervalValueExpression: ((intervalTerm)|(LeftParen datetimeValueExpression MinusSign datetimeTerm RightParen intervalQualifier)) ((PlusSign intervalTerm)|(MinusSign intervalTerm))*;
datetimeTerm: datetimeFactor;
datetimeFactor: datetimePrimary (timeZone)?;
datetimePrimary: (valueExpressionPrimary|datetimeValueFunction);
timeZone: AT timeZoneSpecifier;
timeZoneSpecifier: (LOCAL|TIME ZONE intervalValueExpression);
lengthExpression: (charLengthExpression|octetLengthExpression|bitLengthExpression);
charLengthExpression: (CHAR_LENGTH|CHARACTER_LENGTH) LeftParen stringValueExpression RightParen;
stringValueExpression: (characterValueExpression|bitValueExpression);
octetLengthExpression: OCTET_LENGTH LeftParen stringValueExpression RightParen;
bitLengthExpression: BIT_LENGTH LeftParen stringValueExpression RightParen;
nullSpecification: NULL;
defaultSpecification: DEFAULT;
rowValueConstructorList: rowValueConstructorElement ((Comma rowValueConstructorElement)+)?;
rowSubquery: subquery;
compOp: (EqualsOperator|NotEqualsOperator|LessThanOperator|GreaterThanOperator|LessThanOrEqualsOperator|GreaterThanOrEqualsOperator);
betweenPredicate: rowValueConstructor (NOT)? BETWEEN rowValueConstructor AND rowValueConstructor;
inPredicate: rowValueConstructor (NOT)? IN inPredicateValue;
inPredicateValue: (tableSubquery|LeftParen inValueList RightParen);
inValueList: valueExpression (Comma valueExpression)+;
likePredicate: matchValue (NOT)? LIKE pattern (ESCAPE escapeCharacter)?;
matchValue: characterValueExpression;
pattern: characterValueExpression;
escapeCharacter: characterValueExpression;
nullPredicate: rowValueConstructor IS (NOT)? NULL;
quantifiedComparisonPredicate: rowValueConstructor compOp quantifier tableSubquery;
quantifier: (all|some);
all: ALL;
some: (SOME|ANY);
existsPredicate: EXISTS tableSubquery;
// uniquePredicate: UNIQUE tableSubquery;
matchPredicate: rowValueConstructor MATCH (UNIQUE)? ((PARTIAL|FULL))? tableSubquery;
overlapsPredicate: rowValueConstructor1 OVERLAPS rowValueConstructor2;
rowValueConstructor1: rowValueConstructor;
rowValueConstructor2: rowValueConstructor;
truthValue: (TRUE|FALSE|UNKNOWN);

// constraints
constraintAttributes: (constraintCheckTime ((NOT)? DEFERRABLE)?|(NOT)? DEFERRABLE (constraintCheckTime)?);
constraintCheckTime: (INITIALLY DEFERRED|INITIALLY IMMEDIATE);
tableConstraintDefinition: (constraintNameDefinition)? tableConstraint (constraintCheckTime)?;
tableConstraint: (uniqueConstraintDefinition|referentialConstraintDefinition|checkConstraintDefinition);
uniqueConstraintDefinition: uniqueSpecification LeftParen uniqueColumnList RightParen;
uniqueColumnList: columnNameList;
referentialConstraintDefinition: FOREIGN KEY LeftParen referencingColumns RightParen referencesSpecification;
referencingColumns: referenceColumnList;

// order by
orderByClause: ORDER BY sortSpecificationList;
sortSpecificationList: sortSpecification ((Comma sortSpecification)+)?;
sortSpecification: sortKey (collateClause)? (orderingSpecification)?;
sortKey: (columnReference|UnsignedInteger);
orderingSpecification: (ASC|DESC);


// cursor and procedure
moduleContents: (declareCursor|dynamicDeclareCursor|procedure);
declareCursor: DECLARE cursorName (INSENSITIVE)? (SCROLL)? CURSOR FOR cursorSpecification;
cursorName: identifier;
cursorSpecification: queryExpression (orderByClause)? (updatabilityClause)?;
updatabilityClause: FOR (READ ONLY|UPDATE (OF columnNameList)?);
dynamicDeclareCursor: DECLARE cursorName (INSENSITIVE)? (SCROLL)? CURSOR FOR statementName;
statementName: identifier;

// procedure
procedure: PROCEDURE procedureName parameterDeclarationList Semicolon sqlProcedureStatement Semicolon;
procedureName: identifier;
parameterDeclarationList: LeftParen parameterDeclaration ((Comma parameterDeclaration)+)? RightParen;
parameterDeclaration: (parameterName dataType|statusParameter);
statusParameter: (SQLCODE|SQLSTATE);
sqlProcedureStatement: (sqlSchemaStatement|sqlDataStatement|sqlTransactionStatement|sqlConnectionStatement|sqlSessionStatement|sqlDynamicStatement|sqlDiagnosticsStatement);

// schema definition
sqlSchemaStatement: (sqlSchemaDefinitionStatement|sqlSchemaManipulationStatement);
sqlSchemaDefinitionStatement: (schemaDefinition|tableDefinition|viewDefinition|grantStatement|domainDefinition|characterSetDefinition|collationDefinition|translationDefinition|assertionDefinition);
schemaDefinition: CREATE SCHEMA schemaNameClause (schemaCharacterSetSpecification)? ((schemaElement)+)?;
schemaNameClause: (schemaName|AUTHORIZATION schemaAuthorizationIdentifier|schemaName AUTHORIZATION schemaAuthorizationIdentifier);
schemaAuthorizationIdentifier: authorizationIdentifier;
schemaCharacterSetSpecification: DEFAULT CHARACTER SET characterSetSpecification;
schemaElement: (domainDefinition|tableDefinition|viewDefinition|grantStatement|assertionDefinition|characterSetDefinition|collationDefinition|translationDefinition);

// domain definition
domainDefinition: CREATE DOMAIN domainName (AS)? dataType (defaultClause)? (domainConstraint)? (collateClause)?;
domainConstraint: (constraintNameDefinition)? checkConstraintDefinition (constraintAttributes)?;
tableDefinition: CREATE ((GLOBAL|LOCAL) TEMPORARY)? TABLE tableName tableElementList (ON COMMIT (DELETE|PRESERVE) ROWS)?;
viewDefinition: CREATE VIEW tableName (LeftParen viewColumnList RightParen)? AS queryExpression (WITH (levelsClause)? CHECK OPTION)?;
viewColumnList: columnNameList;
levelsClause: (CASCADED|LOCAL);

// privileges
grantStatement: GRANT privileges ON objectName TO grantee ((Comma grantee)+)? (WITH GRANT OPTION)?;
privileges: (ALL PRIVILEGES|actionList);
actionList: action ((Comma action)+)?;
action: (SELECT|DELETE|INSERT (LeftParen privilegeColumnList RightParen)?|UPDATE (LeftParen privilegeColumnList RightParen)?|REFERENCES (LeftParen privilegeColumnList RightParen)?|USAGE);
privilegeColumnList: columnNameList;
objectName: ((TABLE)? tableName|DOMAIN domainName|COLLATION collationName|CHARACTER SET characterSetName|TRANSLATION translationName);
grantee: (PUBLIC|authorizationIdentifier);

// assertion
assertionDefinition: CREATE ASSERTION constraintName assertionCheck (constraintAttributes)?;
assertionCheck: CHECK LeftParen searchCondition RightParen;
characterSetDefinition: CREATE CHARACTER SET characterSetName (AS)? characterSetSource ((collateClause|limitedCollationDefinition))?;
characterSetSource: GET existingCharacterSetName;
existingCharacterSetName: (characterSetName|schemaCharacterSetName);
schemaCharacterSetName: characterSetName;

// collation
limitedCollationDefinition: COLLATION FROM collationSource;
collationSource: (collatingSequenceDefinition|translationCollation);
collatingSequenceDefinition: (externalCollation|collationName|DESC LeftParen collationName RightParen|DEFAULT);
externalCollation: EXTERNAL LeftParen SingleQuote collationName SingleQuote RightParen;
translationCollation: TRANSLATION translationName (THEN COLLATION collationName)?;
collationDefinition: CREATE COLLATION collationName FOR characterSetSpecification FROM collationSource (padAttribute)?;
padAttribute: (NO PAD|PAD SPACE);
translationDefinition: CREATE TRANSLATION translationName FOR sourceCharacterSetSpecification TO targetCharacterSetSpecification FROM translationSource;
sourceCharacterSetSpecification: characterSetSpecification;
targetCharacterSetSpecification: characterSetSpecification;
translationSource: translationSpecification;
translationSpecification: (externalTranslation|IDENTITY|translationName);
externalTranslation: EXTERNAL LeftParen SingleQuote translationName SingleQuote RightParen;

// schema ddl
sqlSchemaManipulationStatement: (dropSchemaStatement|alterTableStatement|dropTableStatement|dropViewStatement|revokeStatement|alterDomainStatement|dropDomainStatement|dropCharacterSetStatement|dropCollationStatement|dropTranslationStatement|dropAssertionStatement);
dropSchemaStatement: DROP SCHEMA schemaName dropBehaviour;
dropBehaviour: (CASCADE|RESTRICT);
alterTableStatement: ALTER TABLE tableName alterTableAction;
alterTableAction: (addColumnDefinition|alterColumnDefinition|dropColumnDefinition|addTableConstraintDefinition|dropTableConstraintDefinition);
addColumnDefinition: ADD (COLUMN)? columnDefinition;
alterColumnDefinition: ALTER (COLUMN)? columnName alterColumnAction;
alterColumnAction: (setColumnDefaultClause|dropColumnDefaultClause);
setColumnDefaultClause: SET defaultClause;
dropColumnDefaultClause: DROP DEFAULT;
dropColumnDefinition: DROP (COLUMN)? columnName dropBehaviour;
addTableConstraintDefinition: ADD tableConstraintDefinition;
dropTableConstraintDefinition: DROP CONSTRAINT constraintName dropBehaviour;
dropTableStatement: DROP TABLE tableName dropBehaviour;
dropViewStatement: DROP VIEW tableName dropBehaviour;
revokeStatement: REVOKE (GRANT OPTION FOR)? privileges ON objectName FROM grantee ((Comma grantee)+)? dropBehaviour;
alterDomainStatement: ALTER DOMAIN domainName alterDomainAction;
alterDomainAction: (setDomainDefaultClause|dropDomainDefaultClause|addDomainConstraintDefinition|dropDomainConstraintDefinition);
setDomainDefaultClause: SET defaultClause;
dropDomainDefaultClause: DROP DEFAULT;
addDomainConstraintDefinition: ADD domainConstraint;
dropDomainConstraintDefinition: DROP CONSTRAINT constraintName;
dropDomainStatement: DROP DOMAIN domainName dropBehaviour;
dropCharacterSetStatement: DROP CHARACTER SET characterSetName;
dropCollationStatement: DROP COLLATION collationName;
dropTranslationStatement: DROP TRANSLATION translationName;
dropAssertionStatement: DROP ASSERTION constraintName;

// cursor
sqlDataStatement: (openStatement|fetchStatement|closeStatement|selectStatementSingleRow|sqlDataChangeStatement);
openStatement: OPEN cursorName;
fetchStatement: FETCH ((fetchOrientation)? FROM)? cursorName INTO fetchTargetList;
fetchOrientation: (NEXT|PRIOR|FIRST|LAST|(ABSOLUTE|RELATIVE) simpleValueSpecification);
simpleValueSpecification: (parameterName|literal);
fetchTargetList: targetSpecification ((Comma targetSpecification)+)?;
targetSpecification: (parameterSpecification);
closeStatement: CLOSE cursorName;
selectStatementSingleRow: SELECT (setQuantifier)? selectList INTO selectTargetList tableExpression;
selectTargetList: targetSpecification ((Comma targetSpecification)+)?;

// data change statements
sqlDataChangeStatement: (deleteStatementPositioned|deleteStatementSearched|insertStatement|updateStatementPositioned|updateStatementSearched);
deleteStatementPositioned: DELETE FROM tableName WHERE CURRENT OF cursorName;
deleteStatementSearched: DELETE FROM tableName (WHERE searchCondition)?;
insertStatement: INSERT INTO tableName insertColumnsAndSource;
insertColumnsAndSource: ((LeftParen insertColumnList RightParen)? queryExpression|DEFAULT VALUES);
insertColumnList: columnNameList;
updateStatementPositioned: UPDATE tableName SET setClauseList WHERE CURRENT OF cursorName;
setClauseList: setClause ((Comma setClause)+)?;
setClause: objectColumn EqualsOperator updateSource;
objectColumn: columnName;
updateSource: (valueExpression|nullSpecification|DEFAULT);
updateStatementSearched: UPDATE tableName SET setClauseList (WHERE searchCondition)?;

// transactions
sqlTransactionStatement: (setTransactionStatement|setConstraintsModeStatement|commitStatement|rollbackStatement);
setTransactionStatement: SET TRANSACTION transactionMode ((Comma transactionMode)+)?;
transactionMode: (isolationLevel|transactionAccessMode|diagnosticsSize);
isolationLevel: ISOLATION LEVEL levelOfIsolation;
levelOfIsolation: (READ UNCOMMITTED|READ COMMITTED|REPEATABLE READ|SERIALIZABLE);
transactionAccessMode: (READ ONLY|READ WRITE);
diagnosticsSize: DIAGNOSTICS SIZE numberOfConditions;
numberOfConditions: simpleValueSpecification;
setConstraintsModeStatement: SET CONSTRAINTS constraintNameList (DEFERRED|IMMEDIATE);
constraintNameList: (ALL|constraintName ((Comma constraintName)+)?);
commitStatement: COMMIT (WORK)?;
rollbackStatement: ROLLBACK (WORK)?;

// connection
sqlConnectionStatement: (connectStatement|setConnectionStatement|disconnectStatement);
connectStatement: CONNECT TO connectionTarget;
connectionTarget: (sqlServerName (AS connectionName)? (USER userName)?|DEFAULT);
sqlServerName: simpleValueSpecification;
connectionName: simpleValueSpecification;
userName: simpleValueSpecification;
setConnectionStatement: SET CONNECTION connectionObject;
connectionObject: (DEFAULT|connectionName);
disconnectStatement: DISCONNECT disconnectObject;
disconnectObject: (connectionObject|ALL|CURRENT);

// session
sqlSessionStatement: (setCatalogStatement|setSchemaStatement|setNamesStatement|setSessionAuthorizationIdentifierStatement|setLocalTimeZoneStatement);
setCatalogStatement: SET CATALOG valueSpecification;
valueSpecification: (literal|generalValueSpecification);
setSchemaStatement: SET SCHEMA valueSpecification;
setNamesStatement: SET NAMES valueSpecification;
setSessionAuthorizationIdentifierStatement: SET SESSION AUTHORIZATION valueSpecification;
setLocalTimeZoneStatement: SET TIME ZONE setTimeZoneValue;
setTimeZoneValue: (intervalValueExpression|LOCAL);
sqlDynamicStatement: (systemDescriptorStatement|prepareStatement|deallocatePreparedStatement|describeStatement|executeStatement|executeImmediateStatement|sqlDynamicDataStatement);
systemDescriptorStatement: (allocateDescriptorStatement|deallocateDescriptorStatement|getDescriptorStatement|setDescriptorStatement);
allocateDescriptorStatement: ALLOCATE DESCRIPTOR descriptorName (WITH MAX occurrences)?;
descriptorName: (scopeOption)? simpleValueSpecification;
scopeOption: (GLOBAL|LOCAL);
occurrences: simpleValueSpecification;
deallocateDescriptorStatement: DEALLOCATE DESCRIPTOR descriptorName;

// descriptor
setDescriptorStatement: SET DESCRIPTOR descriptorName setDescriptorInformation;
setDescriptorInformation: (setCount|VALUE itemNumber setItemInformation ((Comma setItemInformation)+)?);
setCount: COUNT EqualsOperator simpleValueSpecification1;
simpleValueSpecification1: simpleValueSpecification;
itemNumber: simpleValueSpecification;
setItemInformation: descriptorItemName EqualsOperator simpleValueSpecification2;
descriptorItemName: (TYPE|LENGTH|OCTET_LENGTH|RETURNED_LENGTH|RETURNED_OCTET_LENGTH|PRECISION|SCALE|DATETIME_INTERVAL_CODE|DATETIME_INTERVAL_PRECISION|NULLABLE|INDICATOR|DATA|NAME|UNNAMED|COLLATION_CATALOG|COLLATION_SCHEMA|COLLATION_NAME|CHARACTER_SET_CATALOG|CHARACTER_SET_SCHEMA|CHARACTER_SET_NAME);
simpleValueSpecification2: simpleValueSpecification;
getDescriptorStatement: GET DESCRIPTOR descriptorName getDescriptorInformation;
getDescriptorInformation: (getCount|VALUE itemNumber getItemInformation ((Comma getItemInformation)+)?);
getCount: simpleTargetSpecification1 EqualsOperator COUNT;
simpleTargetSpecification1: simpleTargetSpecification;
simpleTargetSpecification: (parameterName);
getItemInformation: simpleTargetSpecification2 EqualsOperator descriptorItemName;
simpleTargetSpecification2: simpleTargetSpecification;

// prepare statement
prepareStatement: PREPARE sqlStatementName FROM sqlStatementVariable;
sqlStatementName: (statementName|extendedStatementName);
extendedStatementName: (scopeOption)? simpleValueSpecification;
sqlStatementVariable: simpleValueSpecification;
deallocatePreparedStatement: DEALLOCATE PREPARE sqlStatementName;
describeStatement: (describeInputStatement|describeOutputStatement);
describeInputStatement: DESCRIBE INPUT sqlStatementName usingDescriptor;
usingDescriptor: (USING|INTO) SQL DESCRIPTOR descriptorName;
describeOutputStatement: DESCRIBE (OUTPUT)? sqlStatementName usingDescriptor;

// execute statement
executeStatement: EXECUTE sqlStatementName (resultUsingClause)? (parameterUsingClause)?;
resultUsingClause: usingClause;
usingClause: (usingArguments|usingDescriptor);
usingArguments: (USING|INTO) argument ((Comma argument)+)?;
argument: targetSpecification;
parameterUsingClause: usingClause;
executeImmediateStatement: EXECUTE IMMEDIATE sqlStatementVariable;

// dynamic data statements
sqlDynamicDataStatement: (allocateCursorStatement|dynamicOpenStatement|dynamicCloseStatement|dynamicFetchStatement|dynamicDeleteStatementPositioned|dynamicUpdateStatementPositioned);
allocateCursorStatement: ALLOCATE extendedCursorName (INSENSITIVE)? (SCROLL)? CURSOR FOR extendedStatementName;
extendedCursorName: (scopeOption)? simpleValueSpecification;
dynamicOpenStatement: OPEN dynamicCursorName (usingClause)?;
dynamicCursorName: (cursorName|extendedCursorName);
dynamicCloseStatement: CLOSE dynamicCursorName;
dynamicFetchStatement: FETCH ((fetchOrientation)? FROM)? dynamicCursorName;
dynamicDeleteStatementPositioned: DELETE FROM tableName WHERE CURRENT OF dynamicCursorName;
dynamicUpdateStatementPositioned: UPDATE tableName SET setClause ((Comma setClause)+)? WHERE CURRENT OF dynamicCursorName;

// diagnostics statement
sqlDiagnosticsStatement: getDiagnosticsStatement;
getDiagnosticsStatement: GET DIAGNOSTICS sqlDiagnosticsInformation;
sqlDiagnosticsInformation: (statementInformation|conditionInformation);
statementInformation: statementInformationItem ((Comma statementInformationItem)+)?;
statementInformationItem: simpleTargetSpecification EqualsOperator statementInformationItemName;
statementInformationItemName: (NUMBER|MORE_KW|COMMAND_FUNCTION|DYNAMIC_FUNCTION|ROW_COUNT);
conditionInformation: EXCEPTION conditionNumber conditionInformationItem ((Comma conditionInformationItem)+)?;
conditionNumber: simpleValueSpecification;
conditionInformationItem: simpleTargetSpecification EqualsOperator conditionInformationItemName;
conditionInformationItemName: (CONDITION_NUMBER|RETURNED_SQLSTATE|CLASS_ORIGIN|SUBCLASS_ORIGIN|SERVER_NAME|CONNECTION_NAME|CONSTRAINT_CATALOG|CONSTRAINT_SCHEMA|CONSTRAINT_NAME|CATALOG_NAME|SCHEMA_NAME|TABLE_NAME|COLUMN_NAME|CURSOR_NAME|MESSAGE_TEXT|MESSAGE_LENGTH|MESSAGE_OCTET_LENGTH);

// statement or declaration - base rule
statementOrDeclaration: (declareCursor|dynamicDeclareCursor|temporaryTableDeclaration|sqlProcedureStatement);

// preparable statement - base rule
preparableStatement: (preparableSqlDataStatement|preparableSqlSchemaStatement|preparableSqlTransactionStatement|preparableSqlSessionStatement);
preparableSqlDataStatement: (deleteStatementSearched|dynamicSingleRowSelectStatement|insertStatement|dynamicSelectStatement|updateStatementSearched|preparableDynamicDeleteStatementPositioned|preparableDynamicUpdateStatementPositioned);
dynamicSingleRowSelectStatement: querySpecification;
dynamicSelectStatement: cursorSpecification;
preparableDynamicDeleteStatementPositioned: DELETE (FROM tableName)? WHERE CURRENT OF cursorName;
preparableDynamicUpdateStatementPositioned: UPDATE (tableName)? SET setClause WHERE CURRENT OF cursorName;
preparableSqlSchemaStatement: sqlSchemaStatement;
preparableSqlTransactionStatement: sqlTransactionStatement;
preparableSqlSessionStatement: sqlSessionStatement;

// direct statement - base rule
directSqlStatement: (directSqlDataStatement|sqlSchemaStatement|sqlTransactionStatement|sqlConnectionStatement|sqlSessionStatement);
directSqlDataStatement: (deleteStatementSearched|selectStatement|insertStatement|updateStatementSearched|temporaryTableDeclaration);
selectStatement: queryExpression (orderByClause)?;

// root rule for script
sqlQueries: (sqlQuery ';'?)* EOF; // EOF - don't stop early. must match all input
sqlQuery: directSqlStatement|preparableStatement|module|statementOrDeclaration;


nonReserved: ADA
    |    C_ | CATALOG_NAME | CHARACTER_SET_CATALOG | CHARACTER_SET_NAME | CHARACTER_SET_SCHEMA
    |    CLASS_ORIGIN | COBOL | COLLATION_CATALOG | COLLATION_NAME | COLLATION_SCHEMA
    |    COLUMN_NAME | COMMAND_FUNCTION | COMMITTED | CONDITION_NUMBER | CONNECTION_NAME
    |    CONSTRAINT_CATALOG | CONSTRAINT_NAME | CONSTRAINT_SCHEMA | CURSOR_NAME
    |    DATA | DATETIME_INTERVAL_CODE | DATETIME_INTERVAL_PRECISION | DYNAMIC_FUNCTION
    |    FORTRAN
    |    LENGTH
    |    MESSAGE_LENGTH | MESSAGE_OCTET_LENGTH | MESSAGE_TEXT | MORE_KW | MUMPS
    |    NAME | NULLABLE | NUMBER
    |    PASCAL | PLI | PUBLIC
    |    REPEATABLE | RETURNED_LENGTH | RETURNED_OCTET_LENGTH | RETURNED_SQLSTATE | ROW_COUNT
    |    SCALE | SCHEMA_NAME | SERIALIZABLE | SERVER_NAME | SUBCLASS_ORIGIN
    |    TABLE_NAME | TYPE
    |    UNCOMMITTED | UNNAMED;
