/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
     * Copyright (C) 2010-2024 DBeaver Corp and others
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

// root rule for script
sqlQueries: sqlQuery (Semicolon sqlQuery)* Semicolon? EOF; // EOF - don't stop early. must match all input
sqlQuery: directSqlDataStatement|sqlSchemaStatement|sqlTransactionStatement|sqlSessionStatement|sqlDataStatement;

directSqlDataStatement: withClause? (deleteStatement|selectStatement|insertStatement|updateStatement);
selectStatement: queryExpression;

// data type literals
sign: PlusSign|MinusSign;
literal: (signedNumericLiteral|generalLiteral);
unsignedNumericLiteral: (UnsignedInteger|DecimalLiteral|ApproximateNumericLiteral);
signedNumericLiteral: sign? unsignedNumericLiteral;
characterStringLiteral: (Introducer characterSetSpecification)? StringLiteralContent;
generalLiteral: (characterStringLiteral|NationalCharacterStringLiteral|BitStringLiteral|HexStringLiteral|datetimeLiteral|intervalLiteral);
datetimeLiteral: (dateLiteral|timeLiteral|timestampLiteral);
dateLiteral: DATE StringLiteralContent;
timeLiteral: TIME StringLiteralContent;
timestampLiteral: TIMESTAMP StringLiteralContent;
// intervalLiteral: INTERVAL sign? StringLiteralContent intervalQualifier;
intervalLiteral: INTERVAL sign? valueExpressionPrimary intervalQualifier;

// identifiers
characterSetSpecification: characterSetName;
characterSetName: (schemaName Period)? Identifier;
schemaName: (catalogName Period)? unqualifiedSchemaName;
unqualifiedSchemaName: identifier;
qualifiedName: (schemaName Period)? identifier;
catalogName: identifier;
identifier: (Introducer characterSetSpecification)? actualIdentifier;
actualIdentifier: (Identifier|DelimitedIdentifier|nonReserved|Quotted);

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
columnDefinition: columnName dataType (defaultClause)? (columnConstraintDefinition)* (anyWordWithAnyValue)?;
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
columnNameList: columnName (Comma columnName)*;
matchType: (FULL|PARTIAL);
referentialTriggeredAction: (updateRule (deleteRule)?|deleteRule (updateRule)?);
updateRule: ON UPDATE referentialAction;
referentialAction: (CASCADE|SET NULL|SET DEFAULT|NO ACTION);
deleteRule: ON DELETE referentialAction;

// search conditions
searchCondition: (booleanTerm (OR booleanTerm)*)? anyUnexpected??; // (.*?) - for error recovery
booleanTerm: booleanFactor (AND booleanFactor)*;
booleanFactor: (NOT)? booleanTest;
booleanTest: booleanPrimary (IS (NOT)? truthValue)?;
booleanPrimary: (predicate|LeftParen searchCondition RightParen);
predicate: (existsPredicate|likePredicate|rowValuePredicate);

rowValuePredicate: rowValueConstructor (comparisonPredicate|betweenPredicate|inPredicate|nullPredicate|quantifiedComparisonPredicate|matchPredicate|overlapsPredicate);
comparisonPredicate: compOp rowValueConstructor;
betweenPredicate: (NOT)? BETWEEN rowValueConstructor AND rowValueConstructor;
inPredicate: (NOT)? IN inPredicateValue;
nullPredicate: IS (NOT? NULL | NOTNULL);
quantifiedComparisonPredicate: compOp quantifier tableSubquery;
matchPredicate: MATCH (UNIQUE)? (matchType)? tableSubquery;
overlapsPredicate: OVERLAPS rowValueConstructor;

compOp: (EqualsOperator|NotEqualsOperator|LessThanOperator|GreaterThanOperator|LessThanOrEqualsOperator|GreaterThanOrEqualsOperator|Tilda|REGEXP);
quantifier: (ALL|SOME|ANY);
truthValue: (TRUE|FALSE|UNKNOWN);
existsPredicate: EXISTS tableSubquery;
likePredicate: matchValue (NOT)? LIKE pattern (ESCAPE escapeCharacter)?;
matchValue: characterValueExpression;
pattern: characterValueExpression;
escapeCharacter: characterValueExpression;
inPredicateValue: (tableSubquery|(LeftParen (valueExpression (Comma valueExpression)*) RightParen));

rowValueConstructor: (rowValueConstructorElement|LeftParen rowValueConstructorList anyUnexpected?? RightParen|rowSubquery);
rowValueConstructorElement: (valueExpression|nullSpecification|defaultSpecification);
nullSpecification: NULL;
defaultSpecification: DEFAULT;
rowValueConstructorList: rowValueConstructorElement (Comma rowValueConstructorElement)*;
rowSubquery: subquery;
generalValueSpecification: (parameterSpecification|dynamicParameterSpecification|USER|CURRENT_USER|SESSION_USER|SYSTEM_USER|VALUE);
parameterSpecification: parameterName (indicatorParameter)?;
parameterName: Colon identifier;
indicatorParameter: (INDICATOR)? parameterName;
dynamicParameterSpecification: QuestionMark;
columnReference: (qualifier Period)? columnName;
//columnReference: identifier (Period identifier (Period identifier (Period identifier)?)?)?;
qualifier: (tableName|correlationName);
correlationName: identifier;

withClause: WITH RECURSIVE? cteList;
cteList: with_list_element (Comma with_list_element)*;
with_list_element: queryName (LeftParen withColumnList RightParen)? AS anyWordsWithProperty? subquery;
withColumnList: columnName (Comma columnName)*;
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
selectList: selectSublist (Comma selectSublist)*; // (Comma selectSublist)* contains any quantifier for error recovery;
selectSublist: (Asterisk|derivedColumn|qualifier Period Asterisk)? anyUnexpected??; // (.*?) for whole rule to handle select fields autocompletion when from immediately after select
derivedColumn: valueExpression (asClause)?;
asClause: (AS)? columnName;
tableExpression: fromClause whereClause? groupByClause? havingClause? orderByClause? limitClause?;
queryPrimary: (nonJoinQueryPrimary|joinedTable);
queryTerm: (nonJoinQueryTerm|joinedTable);
queryExpression: (joinedTable|nonJoinQueryTerm) (unionTerm|exceptTerm)*;

// from
fromClause: FROM tableReference (Comma tableReference)*;
nonjoinedTableReference: (tableName (PARTITION anyProperty)? (correlationSpecification)?)|(derivedTable correlationSpecification);
tableReference: nonjoinedTableReference|joinedTable|tableReferenceHints|anyUnexpected??; // '.*' to handle incomplete queries
tableReferenceHints: (tableHintKeywords|anyWord)+ anyProperty; // dialect-specific options, should be described and moved to dialects in future
joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
correlationSpecification: (AS)? correlationName (LeftParen derivedColumnList RightParen)?;
derivedColumnList: columnNameList;
derivedTable: tableSubquery;
tableSubquery: subquery;

//joins
crossJoinTerm: CROSS JOIN tableReference;
naturalJoinTerm: (NATURAL)? (joinType)? JOIN tableReference (joinSpecification)? anyUnexpected??; // (.*?) - for error recovery
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
tableValueConstructor: VALUES (rowValueConstructor (Comma rowValueConstructor)*);
explicitTable: TABLE tableName;
correspondingSpec: CORRESPONDING (BY LeftParen correspondingColumnList RightParen)?;
correspondingColumnList: columnNameList;
caseExpression: (caseAbbreviation|simpleCase|searchedCase);
caseAbbreviation: (NULLIF LeftParen valueExpression Comma valueExpression RightParen|COALESCE LeftParen valueExpression (Comma valueExpression)+ RightParen);
simpleCase: CASE valueExpression (simpleWhenClause)+ (elseClause)? END;
simpleWhenClause: WHEN valueExpression THEN result;
result: valueExpression|NULL;
elseClause: ELSE result;
searchedCase: CASE (searchedWhenClause)+ (elseClause)? END;
searchedWhenClause: WHEN searchCondition THEN result;
castSpecification: CAST LeftParen castOperand AS dataType RightParen;
castOperand: (valueExpression|NULL);

overClause: OVER LeftParen (PARTITION BY valueExpression (Comma valueExpression)*)? orderByClause? ((ROWS|RANGE) anyUnexpected)? RightParen;

// functions and operators
//valueExpression: (numericValueExpression|characterValueExpression|datetimeValueExpression|intervalValueExpression);

valueExpression: valueExpressionPrimaryBased|extractExpressionBased|anyWordsWithPropertyBased
                  |valueExpressionPrimarySignedBased|extractExpressionSignedBased|anyWordsWithPropertySignedBased
                  |intervalExpressionBased|(sign? valueExpressionPrimary);

valueExpressionPrimarySignedBased: sign valueExpressionPrimary (                       numericOperation|intervalOperation|intervalOperation2);
      valueExpressionPrimaryBased:      valueExpressionPrimary (concatenationOperation|numericOperation|intervalOperation|intervalOperation2);
     extractExpressionSignedBased: sign extractExpression      (                       numericOperation|                  intervalOperation2);
           extractExpressionBased:      extractExpression      (                       numericOperation|                  intervalOperation2);
  anyWordsWithPropertySignedBased: sign anyWordsWithProperty   (                       numericOperation|                  intervalOperation2);
        anyWordsWithPropertyBased:      anyWordsWithProperty   (concatenationOperation|numericOperation|                  intervalOperation2);
          intervalExpressionBased: (LeftParen datetimeValueExpression MinusSign anyWordsWithProperty RightParen intervalQualifier) (sign intervalTerm)*;

concatenationOperation: (ConcatenationOperator characterPrimary)+;
numericOperation:                           (((Asterisk|Solidus) factor)+ (sign term)*)|((sign term)+);

intervalOperation:       intervalQualifier?((((Asterisk|Solidus) factor)+ (sign intervalTerm)*)|((sign intervalTerm)+));
intervalOperation2: Asterisk intervalFactor((((Asterisk|Solidus) factor)+ (sign intervalTerm)*)|((sign intervalTerm)+));

valueExpressionPrimary: unsignedNumericLiteral|generalLiteral|generalValueSpecification|countAllExpression
    |scalarSubquery|caseExpression|LeftParen valueExpression anyUnexpected?? RightParen|castSpecification
    |aggregateExpression|anyWordsWithProperty2|columnReference|anyWordsWithProperty;


numericPrimary: (valueExpressionPrimary|extractExpression|anyWordsWithProperty);
factor: sign? numericPrimary;
term: factor ((Asterisk|Solidus) factor)*;
numericValueExpression: term (sign term)*;

characterPrimary: (valueExpressionPrimary|anyWordsWithProperty);
characterValueExpression: characterPrimary (ConcatenationOperator characterPrimary)*;

intervalPrimary: valueExpressionPrimary (intervalQualifier)?;
intervalFactor: sign? intervalPrimary;
intervalTerm: ((intervalFactor)|(term Asterisk intervalFactor)) ((Asterisk|Solidus) factor)*;
intervalValueExpression: ((intervalTerm)|(LeftParen datetimeValueExpression MinusSign anyWordsWithProperty RightParen intervalQualifier)) (sign intervalTerm)*;

datetimeValueExpression: (anyWordsWithProperty|(intervalValueExpression PlusSign anyWordsWithProperty)) (sign intervalTerm)*;
extractSource: (datetimeValueExpression|intervalValueExpression); // L

countAllExpression: COUNT LeftParen Asterisk RightParen;
extractExpression: EXTRACT LeftParen extractField FROM extractSource RightParen;
extractField: (datetimeField|timeZoneField);
datetimeField: (nonSecondDatetimeField|SECOND);
timeZoneField: (TIMEZONE_HOUR|TIMEZONE_MINUTE);

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
limitClause: LIMIT valueExpression (OFFSET valueExpression)? (Comma valueExpression)?;
sortSpecificationList: sortSpecification (Comma sortSpecification)*;
sortSpecification: sortKey (orderingSpecification)?;
sortKey: columnReference | UnsignedInteger | anyWordsWithProperty;
orderingSpecification: (ASC|DESC);

// schema definition
sqlSchemaStatement: (sqlSchemaDefinitionStatement|sqlSchemaManipulationStatement);
sqlSchemaDefinitionStatement: (schemaDefinition|tableDefinition|viewDefinition);
schemaDefinition: CREATE SCHEMA schemaNameClause (schemaCharacterSetSpecification)? (schemaElement)*;
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
tableElementList: LeftParen tableElement (Comma tableElement)* RightParen;
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
selectTargetList: parameterSpecification (Comma parameterSpecification)*;
sqlDataChangeStatement: (deleteStatement|insertStatement|updateStatement);
deleteStatement: DELETE FROM tableName whereClause?;
insertStatement: INSERT INTO tableName? insertColumnsAndSource;
insertColumnsAndSource: ((LeftParen (insertColumnList?|Asterisk) RightParen?)? queryExpression|DEFAULT VALUES)?;
insertColumnList: columnNameList;

// UPDATE
updateStatement: UPDATE anyWordsWithProperty?? tableReference? (SET setClauseList? fromClause? whereClause? orderByClause? limitClause? anyWordsWithProperty??)?;
setClauseList: setClause (Comma setClause)*;
setClause: ((setTarget | setTargetList) (EqualsOperator updateSource)?)|anyUnexpected??;
setTarget: columnReference;
setTargetList: LeftParen columnReference? (Comma columnReference)* RightParen?;
updateSource: updateValue | (LeftParen updateValue (Comma updateValue)* RightParen?);
updateValue: valueExpression|nullSpecification|DEFAULT;

// transactions
sqlTransactionStatement: (setTransactionStatement|setConstraintsModeStatement|commitStatement|rollbackStatement);
setTransactionStatement: SET TRANSACTION transactionMode (Comma transactionMode)*;
transactionMode: (isolationLevel|transactionAccessMode);
isolationLevel: ISOLATION LEVEL levelOfIsolation;
levelOfIsolation: (READ UNCOMMITTED|READ COMMITTED|REPEATABLE READ|SERIALIZABLE);
transactionAccessMode: (READ ONLY|READ WRITE);
setConstraintsModeStatement: SET CONSTRAINTS constraintNameList (DEFERRED|IMMEDIATE);
constraintNameList: (ALL|constraintName (Comma constraintName)*);
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
anyValue: rowValueConstructor|searchCondition;
anyWordWithAnyValue: anyWord anyValue;
anyProperty: LeftParen (anyValue (Comma anyValue)*) RightParen;
anyWordsWithProperty: anyWord+ anyProperty?;
anyWordsWithProperty2: anyWord+ anyProperty overClause?;

aggregateExpression: actualIdentifier LeftParen aggregateExprParam+ RightParen (WITHIN GROUP LeftParen orderByClause RightParen)? (FILTER LeftParen WHERE searchCondition RightParen)?;
aggregateExprParam: DISTINCT|ALL|ORDER|BY|ASC|DESC|LIMIT|SEPARATOR|OFFSET|rowValueConstructor;

/*
All the logical boundary terms between query construct levels should be explicitly mentioned here for the anyUnexpected to NOT cross them
MATCH path = (a:RuleEnd)-[r:BeginAlternative|EndAlternativeBranch|EnterRule|ExitRule|Skip|Default*]->(b)-[t:Term]->(c),rrr2 = (f:RuleContainer)-[:RuleContains]->(a)
WHERE f.payload in ['naturalJoinTerm','searchCondition','selectSublist','tableReference'] return distinct t.payload
*/
anyUnexpected: ((~(LeftParen|RightParen|Period|Semicolon|Comma|
THEN|GROUP|HAVING|UNION|EXCEPT|WITH|INTERSECT|ORDER|ON|USING|WHERE|INTO|FROM
))|(identifier Period)|(LeftParen anyUnexpected* RightParen))+;

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
