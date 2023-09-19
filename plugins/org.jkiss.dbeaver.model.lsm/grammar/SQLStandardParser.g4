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

// root rule for script
sqlQueries: sqlQuery (Semicolon sqlQuery)* Semicolon? EOF; // EOF - don't stop early. must match all input
sqlQuery: directSqlDataStatement|sqlSchemaStatement|sqlTransactionStatement|sqlSessionStatement|sqlDataStatement;

directSqlDataStatement: (deleteStatement|selectStatement|insertStatement|updateStatement);
selectStatement: withClause? queryExpression orderByClause?;

// data type literals
literal: (signedNumericLiteral|generalLiteral);
unsignedNumericLiteral: (UnsignedInteger|DecimalLiteral|ApproximateNumericLiteral);
signedNumericLiteral: (Sign)? unsignedNumericLiteral;
characterStringLiteral: (Introducer characterSetSpecification)? StringLiteralContent;
generalLiteral: (characterStringLiteral|NationalCharacterStringLiteral|BitStringLiteral|HexStringLiteral|datetimeLiteral|intervalLiteral);
datetimeLiteral: (dateLiteral|timeLiteral|timestampLiteral);
dateLiteral: DATE StringLiteralContent;
timeLiteral: TIME StringLiteralContent;
timestampLiteral: TIMESTAMP StringLiteralContent;
intervalLiteral: INTERVAL (Sign)? StringLiteralContent intervalQualifier;

// identifiers
characterSetSpecification: characterSetName;
characterSetName: (schemaName Period)? Identifier;
schemaName: (catalogName Period)? unqualifiedSchemaName;
unqualifiedSchemaName: identifier;
qualifiedName: (schemaName Period)? identifier;
catalogName: identifier;
identifier: (Introducer characterSetSpecification)? actualIdentifier;
actualIdentifier: (Identifier|DelimitedIdentifier|squareBracketIdentifier|nonReserved);
squareBracketIdentifier: {isSupportSquareBracketQuotation()}? LeftBracket (~RightBracket|(RightBracket RightBracket))* RightBracket;

// data types
dataType: (datetimeType|intervalType|anyWordsWithProperty (CHARACTER SET characterSetSpecification)?);
datetimeType: (DATE|TIME (LeftParen UnsignedInteger RightParen)? (WITH TIME ZONE)?|TIMESTAMP (LeftParen UnsignedInteger RightParen)? (WITH TIME ZONE)?);
intervalType: INTERVAL intervalQualifier;
intervalQualifier: (startField TO endField|singleDatetimeField);
startField: nonSecondDatetimeField (LeftParen intervalLeadingFieldPrecision RightParen)?;
nonSecondDatetimeField: (YEAR|MONTH|DAY|HOUR|MINUTE);
intervalLeadingFieldPrecision: UnsignedInteger;
endField: (nonSecondDatetimeField|SECOND (LeftParen intervalFractionalSecondsPrecision RightParen)?);
intervalFractionalSecondsPrecision: UnsignedInteger;
singleDatetimeField: (nonSecondDatetimeField (LeftParen intervalLeadingFieldPrecision RightParen)?|SECOND (LeftParen intervalLeadingFieldPrecision (Comma intervalFractionalSecondsPrecision)? RightParen)?);

// column definition
columnDefinition: columnName dataType (defaultClause)? ((columnConstraintDefinition)+)? (anyWordWithAnyValue)?;
columnName: identifier;

// default
defaultClause: DEFAULT anyWordsWithProperty;

// column constraints
columnConstraintDefinition: (constraintNameDefinition)? columnConstraint (constraintAttributes)?;
constraintNameDefinition: CONSTRAINT constraintName;
constraintName: qualifiedName;
columnConstraint: (NOT NULL|UNIQUE|PRIMARY KEY|referencesSpecification|checkConstraintDefinition);
checkConstraintDefinition: CHECK LeftParen searchCondition RightParen;

// references
referencesSpecification: REFERENCES referencedTableAndColumns (MATCH matchType)? (referentialTriggeredAction)?;
referencedTableAndColumns: tableName (LeftParen referenceColumnList RightParen)?;
tableName: qualifiedName;
referenceColumnList: columnNameList;
columnNameList: columnName ((Comma columnName)+)?;
matchType: (FULL|PARTIAL);
referentialTriggeredAction: (updateRule (deleteRule)?|deleteRule (updateRule)?);
updateRule: ON UPDATE referentialAction;
referentialAction: (CASCADE|SET NULL|SET DEFAULT|NO ACTION);
deleteRule: ON DELETE referentialAction;

// search conditions
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
stringValueExpression: (characterValueExpression|bitValueExpression);
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
setFunctionSpecification: (COUNT LeftParen Asterisk RightParen|anyWordsWithProperty);

withClause: WITH RECURSIVE? cteList;
cteList: with_list_element (Comma with_list_element)*;
with_list_element: queryName (LeftParen withColumnList RightParen)? AS anyWordsWithProperty? subquery;
withColumnList: columnName (Comma columnName )*;
queryName: Identifier;

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
setQuantifier: (DISTINCT|ALL);
selectList: (Asterisk|selectSublist) (Comma selectSublist)*; // (Comma selectSublist)* contains any quantifier for error recovery;
selectSublist: (qualifier Period Asterisk | derivedColumn | (.*?)); // (.*?) for whole rule to handle select fields autocompletion when from immediately after select
derivedColumn: valueExpression (asClause)?;
asClause: (AS)? columnName;
tableExpression: fromClause (whereClause)? (groupByClause)? (havingClause)?;
queryPrimary: (nonJoinQueryPrimary|joinedTable);
queryTerm: (nonJoinQueryTerm|joinedTable);
queryExpression: (joinedTable|nonJoinQueryTerm) (unionTerm|exceptTerm)*;

// from
fromClause: FROM tableReference (Comma tableReference)*;
nonjoinedTableReference: (tableName (PARTITION anyProperty)? (correlationSpecification)?)|(derivedTable correlationSpecification);
tableReference: nonjoinedTableReference|joinedTable|tableReferenceHints|(.*?); // '.*' to handle incomplete queries
tableReferenceHints: (tableHintKeywords|anyWord)+ anyProperty; // dialect-specific options, should be described and moved to dialects in future
joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
correlationSpecification: (AS)? correlationName (LeftParen derivedColumnList RightParen)?;
derivedColumnList: columnNameList;
derivedTable: tableSubquery;
tableSubquery: subquery;

//joins
crossJoinTerm: CROSS JOIN tableReference;
naturalJoinTerm: (NATURAL)? (joinType)? JOIN tableReference (joinSpecification)? (.*?); // (.*?) - for error recovery
joinType: (INNER|outerJoinType (OUTER)?|UNION);
outerJoinType: (LEFT|RIGHT|FULL);
joinSpecification: (joinCondition|namedColumnsJoin);
joinCondition: ON searchCondition;
namedColumnsJoin: USING LeftParen joinColumnList RightParen;
joinColumnList: columnNameList;

// conditions
whereClause: WHERE searchCondition;
groupByClause: GROUP BY groupingColumnReferenceList;
groupingColumnReferenceList: groupingColumnReference (Comma groupingColumnReference)*;
groupingColumnReference: columnReference | anyWordsWithProperty;
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
castSpecification: CAST LeftParen castOperand AS dataType RightParen;
castOperand: (valueExpression|NULL);
numericValueFunction: (extractExpression|anyWordsWithProperty);
characterValueExpression: (concatenation|(valueExpressionPrimary|stringValueFunction));
concatenation: (valueExpressionPrimary|stringValueFunction) (ConcatenationOperator (valueExpressionPrimary|stringValueFunction))+;

// functions and operators
stringValueFunction: anyWordsWithProperty;
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
datetimeTerm: anyWordsWithProperty;
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
nullPredicate: rowValueConstructor IS (NOT? NULL | NOTNULL);
quantifiedComparisonPredicate: rowValueConstructor compOp quantifier tableSubquery;
quantifier: (all|some);
all: ALL;
some: (SOME|ANY);
existsPredicate: EXISTS tableSubquery;
matchPredicate: rowValueConstructor MATCH (UNIQUE)? (matchType)? tableSubquery;
overlapsPredicate: rowValueConstructor1 OVERLAPS rowValueConstructor2;
rowValueConstructor1: rowValueConstructor;
rowValueConstructor2: rowValueConstructor;
truthValue: (TRUE|FALSE|UNKNOWN);

// constraints
constraintAttributes: (constraintCheckTime ((NOT)? DEFERRABLE)?|(NOT)? DEFERRABLE (constraintCheckTime)?);
constraintCheckTime: (INITIALLY DEFERRED|INITIALLY IMMEDIATE);
tableConstraintDefinition: (constraintNameDefinition)? tableConstraint (constraintCheckTime)?;
tableConstraint: (uniqueConstraintDefinition|referentialConstraintDefinition|checkConstraintDefinition);
uniqueConstraintDefinition: (UNIQUE|PRIMARY KEY) LeftParen uniqueColumnList RightParen;
uniqueColumnList: columnNameList;
referentialConstraintDefinition: FOREIGN KEY LeftParen referencingColumns RightParen referencesSpecification;
referencingColumns: referenceColumnList;

// order by
orderByClause: ORDER BY sortSpecificationList;
sortSpecificationList: sortSpecification ((Comma sortSpecification)+)?;
sortSpecification: sortKey (orderingSpecification)?;
sortKey: columnReference | UnsignedInteger | anyWordsWithProperty;
orderingSpecification: (ASC|DESC);

// schema definition
sqlSchemaStatement: (sqlSchemaDefinitionStatement|sqlSchemaManipulationStatement);
sqlSchemaDefinitionStatement: (schemaDefinition|tableDefinition|viewDefinition);
schemaDefinition: CREATE SCHEMA schemaNameClause (schemaCharacterSetSpecification)? ((schemaElement)+)?;
schemaNameClause: (schemaName|AUTHORIZATION schemaAuthorizationIdentifier|schemaName AUTHORIZATION schemaAuthorizationIdentifier);
schemaAuthorizationIdentifier: authorizationIdentifier;
authorizationIdentifier: identifier;
schemaCharacterSetSpecification: DEFAULT CHARACTER SET characterSetSpecification;
schemaElement: (tableDefinition|viewDefinition);

// table definition
tableDefinition: CREATE ((GLOBAL|LOCAL) TEMPORARY)? TABLE tableName tableElementList (ON COMMIT (DELETE|PRESERVE) ROWS)?;
viewDefinition: CREATE VIEW tableName (LeftParen viewColumnList RightParen)? AS queryExpression (WITH (levelsClause)? CHECK OPTION)?;
viewColumnList: columnNameList;
levelsClause: (CASCADED|LOCAL);
tableElementList: LeftParen tableElement ((Comma tableElement)+)? RightParen;
tableElement: (columnDefinition|tableConstraintDefinition);

// schema ddl
sqlSchemaManipulationStatement: (dropSchemaStatement|alterTableStatement|dropTableStatement|dropViewStatement|dropCharacterSetStatement);
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
dropCharacterSetStatement: DROP CHARACTER SET characterSetName;

// data statements
sqlDataStatement: (selectStatementSingleRow|sqlDataChangeStatement);
selectStatementSingleRow: SELECT (setQuantifier)? selectList INTO selectTargetList tableExpression;
selectTargetList: parameterSpecification ((Comma parameterSpecification)+)?;
sqlDataChangeStatement: (deleteStatement|insertStatement|updateStatement);
deleteStatement: DELETE FROM tableName (WHERE searchCondition)?;
insertStatement: INSERT INTO tableName insertColumnsAndSource;
insertColumnsAndSource: ((LeftParen insertColumnList RightParen)? queryExpression|DEFAULT VALUES);
insertColumnList: columnNameList;
updateStatement: UPDATE tableName SET setClauseList (WHERE searchCondition)?;
setClauseList: setClause ((Comma setClause)+)?;
setClause: objectColumn EqualsOperator updateSource;
objectColumn: columnName;
updateSource: (valueExpression|nullSpecification|DEFAULT);

// transactions
sqlTransactionStatement: (setTransactionStatement|setConstraintsModeStatement|commitStatement|rollbackStatement);
setTransactionStatement: SET TRANSACTION transactionMode ((Comma transactionMode)+)?;
transactionMode: (isolationLevel|transactionAccessMode);
isolationLevel: ISOLATION LEVEL levelOfIsolation;
levelOfIsolation: (READ UNCOMMITTED|READ COMMITTED|REPEATABLE READ|SERIALIZABLE);
transactionAccessMode: (READ ONLY|READ WRITE);
setConstraintsModeStatement: SET CONSTRAINTS constraintNameList (DEFERRED|IMMEDIATE);
constraintNameList: (ALL|constraintName ((Comma constraintName)+)?);
commitStatement: COMMIT (WORK)?;
rollbackStatement: ROLLBACK (WORK)?;

// session
sqlSessionStatement: (setCatalogStatement|setSchemaStatement|setNamesStatement|setSessionAuthorizationIdentifierStatement|setLocalTimeZoneStatement);
setCatalogStatement: SET CATALOG (identifier|valueSpecification);
valueSpecification: (literal|generalValueSpecification);
setSchemaStatement: SET SCHEMA (identifier|valueSpecification);
setNamesStatement: SET NAMES (identifier|valueSpecification);
setSessionAuthorizationIdentifierStatement: SET SESSION AUTHORIZATION valueSpecification;
setLocalTimeZoneStatement: SET TIME ZONE setTimeZoneValue;
setTimeZoneValue: (intervalValueExpression|LOCAL);

// unknown keyword, data type or function name
anyWord: actualIdentifier;
anyValue: qualifiedName|literal|valueExpression|Comma;
anyWordWithAnyValue: anyWord anyValue;
anyProperty: LeftParen anyValue+ RightParen;
anyWordsWithProperty: anyWord+ anyProperty?;

tableHintKeywords: WITH | UPDATE | IN | KEY | JOIN | ORDER BY | GROUP BY;

nonReserved: COMMITTED | REPEATABLE | SERIALIZABLE | TYPE | UNCOMMITTED |
    CURRENT_USER | SESSION_USER | SYSTEM_USER | USER | VALUE |
    DATE | YEAR | MONTH | DAY | HOUR | MINUTE | SECOND | ZONE |
    ACTION | ADD | AUTHORIZATION | BY | CASCADE | CASCADED | CATALOG | COALESCE | COMMIT |
    CONSTRAINT | CONSTRAINTS | CORRESPONDING | COUNT | DEFERRABLE | DEFERRED | IMMEDIATE |
    EXTRACT | FULL | GLOBAL | LOCAL | INDICATOR | INITIALLY | INTERVAL | ISOLATION | KEY | LEVEL |
    NAMES | NO | NULLIF| ONLY | OVERLAPS| PARTIAL | PRESERVE | READ | RESTRICT | ROLLBACK | SCHEMA |
    SESSION | SET | TEMPORARY | TIME | TIMESTAMP | TIMEZONE_HOUR | TIMEZONE_MINUTE | TRANSACTION |
    VIEW | WORK | WRITE
;
