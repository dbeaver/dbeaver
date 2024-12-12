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

    import org.jkiss.dbeaver.model.lsm.*;
}

@parser::members {
    private boolean isAnonymousParametersEnabled;
    private boolean isNamedParametersEnabled;

    public SQLStandardParser(TokenStream input, LSMAnalyzerParameters parameters) {
        this(input);
        this.isAnonymousParametersEnabled = parameters.isAnonymousSqlParametersEnabled();
        this.isNamedParametersEnabled = parameters.isSqlParametersEnabled();
    }
}

// root rule for script
sqlQueries: sqlQuery (Semicolon sqlQuery)* Semicolon? EOF; // EOF - don't stop early. must match all input
sqlQuery: directSqlDataStatement|sqlSchemaStatement|sqlTransactionStatement|sqlSessionStatement|selectStatementSingleRow;

directSqlDataStatement: withClause? (deleteStatement|selectStatement|insertStatement|updateStatement);
selectStatement: queryExpression;

// data type literals
sign: PlusSign|MinusSign;
literal: (signedNumericLiteral|generalLiteral);
unsignedNumericLiteral: (UnsignedInteger|DecimalLiteral|ApproximateNumericLiteral);
signedNumericLiteral: sign? unsignedNumericLiteral;
characterStringLiteral: ((Introducer characterSetSpecification)? StringLiteralContent)|NationalCharacterStringLiteral|BitStringLiteral|HexStringLiteral;
generalLiteral: characterStringLiteral|datetimeLiteral|intervalLiteral;
datetimeLiteral: dateLiteral|timeLiteral|timestampLiteral;
dateLiteral: DATE StringLiteralContent;
timeLiteral: TIME StringLiteralContent;
timestampLiteral: TIMESTAMP StringLiteralContent;
// intervalLiteral: INTERVAL sign? StringLiteralContent intervalQualifier;
intervalLiteral: INTERVAL sign? valueExpressionPrimary intervalQualifier;

// identifiers
characterSetSpecification: qualifiedName;
characterSetName: qualifiedName;
schemaName: qualifiedName;

qualifiedName: identifier (Period identifier)* Period??;
identifier: (Introducer characterSetSpecification)? actualIdentifier;
actualIdentifier: (Identifier|DelimitedIdentifier|nonReserved|Quotted);

// data types
dataType: (arrayType|datetimeType|intervalType|qualifiedName|anyWordsWithProperty (CHARACTER SET characterSetSpecification)?);
arrayType: ARRAY LeftParen dataType RightParen; // TODO parse it conditionally on the database engine?
datetimeType: DATE|TIME (LeftParen UnsignedInteger RightParen)? (WITH TIME ZONE)?|TIMESTAMP (LeftParen UnsignedInteger RightParen)? (WITH TIME ZONE)?;
intervalType: INTERVAL intervalQualifier;
intervalQualifier: (startField TO endField|singleDatetimeField);
startField: nonSecondDatetimeField (LeftParen intervalLeadingFieldPrecision RightParen)?;
nonSecondDatetimeField: (YEAR|MONTH|DAY|HOUR|MINUTE);
intervalLeadingFieldPrecision: UnsignedInteger;
endField: (nonSecondDatetimeField|SECOND (LeftParen intervalFractionalSecondsPrecision RightParen)?);
intervalFractionalSecondsPrecision: UnsignedInteger;
singleDatetimeField: (nonSecondDatetimeField (LeftParen intervalLeadingFieldPrecision RightParen)?|SECOND (LeftParen intervalLeadingFieldPrecision (Comma intervalFractionalSecondsPrecision)? RightParen)?);

// column definition
columnDefinition: columnName dataType defaultClause? columnConstraintDefinition* anyWordWithAnyValue?; // why anyWordWithAnyValue?
columnName: identifier;

// default
defaultClause: DEFAULT? valueExpression;

// column constraints
columnConstraintDefinition: (constraintNameDefinition)? columnConstraint (constraintAttributes)?;
constraintNameDefinition: CONSTRAINT constraintName;
constraintName: qualifiedName;
columnConstraint: columnConstraintNotNull|columnConstraintUnique|columnConstraintPrimaryKey|referencesSpecification|checkConstraintDefinition;
columnConstraintNotNull: NOT NULL;
columnConstraintUnique: UNIQUE;
columnConstraintPrimaryKey: PRIMARY KEY;
checkConstraintDefinition: CHECK LeftParen searchCondition RightParen;

// references
referencesSpecification: REFERENCES referencedTableAndColumns (MATCH matchType)? referentialTriggeredAction?;
referencedTableAndColumns: tableName (LeftParen referenceColumnList RightParen)?;
tableName: qualifiedName;
referenceColumnList: columnNameList;
columnNameList: columnName (Comma columnName)*;
matchType: FULL|PARTIAL;
referentialTriggeredAction: updateRule deleteRule? | deleteRule updateRule?;
updateRule: ON UPDATE referentialAction;
referentialAction: CASCADE|SET NULL|SET DEFAULT|NO ACTION;
deleteRule: ON DELETE referentialAction;

// search conditions
searchCondition: (booleanTerm (OR booleanTerm)*)? anyUnexpected??; // (.*?) - for error recovery
booleanTerm: booleanFactor (AND booleanFactor)*;
booleanFactor: (NOT)? booleanTest;
booleanTest: booleanPrimary (IS (NOT)? truthValue)?;
booleanPrimary: (predicate|LeftParen searchCondition RightParen|truthValue);
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
likePredicate: matchValue (NOT)? (LIKE|ILIKE) pattern? (ESCAPE escapeCharacter)?;
matchValue: characterValueExpression;
pattern: characterValueExpression;
escapeCharacter: characterValueExpression;
inPredicateValue: (tableSubquery|(LeftParen (valueExpression (Comma valueExpression)*) RightParen));

rowValueConstructor: (rowValueConstructorElement|LeftParen rowValueConstructorList anyUnexpected?? RightParen|rowSubquery);
rowValueConstructorElement: (valueExpression|defaultSpecification);
nullSpecification: NULL;
defaultSpecification: DEFAULT;
rowValueConstructorList: rowValueConstructorElement (Comma rowValueConstructorElement)*;
rowSubquery: subquery;
generalValueSpecification: (parameterSpecification|dynamicParameterSpecification|USER|CURRENT_USER|SESSION_USER|SYSTEM_USER|VALUE);
parameterSpecification: parameterName (indicatorParameter)?;
parameterName: Colon identifier;
indicatorParameter: (INDICATOR)? parameterName;
dynamicParameterSpecification: QuestionMark;
columnReference: (tableName tupleRefSuffix) | qualifiedName;
tupleRefSuffix: Period Asterisk;
//columnReference: identifier (Period identifier (Period identifier (Period identifier)?)?)?;
valueReference: (columnReference|valueRefNestedExpr) valueRefIndexingStep* (valueRefMemberStep valueRefIndexingStep*)*;
valueRefNestedExpr: LeftParen valueReference RightParen;
valueRefIndexingStep: LeftBracket (valueRefIndexingStepDirect|valueRefIndexingStepSlice) RightBracket;
valueRefIndexingStepDirect: signedNumericLiteral;
valueRefIndexingStepSlice: signedNumericLiteral? Colon signedNumericLiteral?;
valueRefMemberStep: Period identifier;
correlationName: identifier;

withClause: WITH RECURSIVE? cteList;
cteList: with_list_element (Comma with_list_element)*;
with_list_element: queryName (LeftParen columnNameList RightParen)? AS anyWordsWithProperty? subquery;
queryName: identifier;

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
setQuantifier: DISTINCT | ALL | UNIQUE;
selectList: selectSublist (Comma selectSublist)*; // (Comma selectSublist)* contains any quantifier for error recovery;
selectSublist: (Asterisk|derivedColumn)? anyUnexpected??; // (.*?) for whole rule to handle select fields autocompletion when from immediately after select
derivedColumn: valueExpression (asClause)?;
asClause: (AS)? columnName;
tableExpression: fromClause whereClause? groupByClause? havingClause? orderByClause? limitClause?;
queryPrimary: (nonJoinQueryPrimary|joinedTable);
queryTerm: (nonJoinQueryTerm|joinedTable);
queryExpression: (joinedTable|nonJoinQueryTerm) (unionTerm|exceptTerm)*;

// from
fromClause: FROM tableReference (Comma tableReference)*;
nonjoinedTableReference: ((tableName (PARTITION anyProperty)?)|derivedTable) correlationSpecification??;
tableReference: nonjoinedTableReference|joinedTable|tableReferenceHints|anyUnexpected??; // '.*' to handle incomplete queries
tableReferenceHints: (tableHintKeywords|anyWord)+ anyProperty; // dialect-specific options, should be described and moved to dialects in future
joinedTable: (nonjoinedTableReference|(LeftParen joinedTable RightParen)) (naturalJoinTerm|crossJoinTerm)+;
correlationSpecification: (AS)? correlationName (LeftParen derivedColumnList RightParen)?;
derivedColumnList: columnNameList;
derivedTable: tableSubquery;
tableSubquery: subquery;

//joins
crossJoinTerm: CROSS JOIN tableReference;
naturalJoinTerm: (NATURAL)? (joinType)? JOIN tableReference (joinSpecification|anyUnexpected)?; // (.*?) - for error recovery
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
groupingColumnReference: columnIndex | valueReference | anyWordsWithProperty;
havingClause: HAVING searchCondition;
tableValueConstructor: VALUES (rowValueConstructor (Comma rowValueConstructor)*);
explicitTable: TABLE tableName?;
correspondingSpec: CORRESPONDING (BY LeftParen correspondingColumnList RightParen)?;
correspondingColumnList: columnNameList;
caseExpression: (caseAbbreviation|simpleCase|searchedCase);
caseAbbreviation: (NULLIF LeftParen valueExpression Comma valueExpression RightParen|COALESCE LeftParen valueExpression (Comma valueExpression)+ RightParen);
simpleCase: CASE (valueExpression|searchCondition) (simpleWhenClause)+ (elseClause)? END;
simpleWhenClause: WHEN (valueExpression|searchCondition) THEN result;
result: valueExpression|searchCondition;
elseClause: ELSE result;
searchedCase: CASE (searchedWhenClause)+ (elseClause)? END;
searchedWhenClause: WHEN searchCondition THEN result;
castSpecification: CAST LeftParen castOperand AS dataType RightParen;
castOperand: valueExpression;

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

valueExpressionPrimary: valueExpressionAtom valueExpressionAttributeSpec? valueExpressionCastSpec?;
valueExpressionAttributeSpec: Colon Identifier; // https://docs.snowflake.com/en/user-guide/tutorials/json-basics-tutorial
valueExpressionCastSpec: TypeCast dataType;
valueExpressionAtom: unsignedNumericLiteral|generalLiteral|countAllExpression
    |scalarSubquery|caseExpression|LeftParen valueExpression anyUnexpected?? RightParen|castSpecification
    |aggregateExpression|nullSpecification|truthValue|variableExpression|generalValueSpecification|anyWordsWithProperty2|valueReference|anyWordsWithProperty;

variableExpression: BatchVariableName|ClientVariableName|namedParameter|anonymouseParameter;
namedParameter: {isNamedParametersEnabled}? (Colon|CustomNamedParameterPrefix) Identifier;
anonymouseParameter: {isAnonymousParametersEnabled}? (QuestionMark|CustomAnonymousParameterMark);

numericPrimary: (valueExpressionPrimary|extractExpression);
factor: sign? numericPrimary;
term: factor ((Asterisk|Solidus) factor)*;
// numericValueExpression: term (sign term)*;

characterPrimary: (valueExpressionPrimary);
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
sortKey: valueReference | columnIndex | anyWordsWithProperty;
columnIndex: UnsignedInteger;
orderingSpecification: (ASC|DESC);

// schema definition
sqlSchemaStatement: schemaDefinition|
    createTableStatement|createViewStatement|alterTableStatement|
    dropSchemaStatement|dropTableStatement|dropViewStatement|dropProcedureStatement|dropCharacterSetStatement;
schemaDefinition: CREATE SCHEMA (IF NOT EXISTS)? schemaNameClause schemaCharacterSetSpecification? schemaElement*;
schemaNameClause: schemaName|AUTHORIZATION schemaAuthorizationIdentifier|schemaName AUTHORIZATION schemaAuthorizationIdentifier;
schemaAuthorizationIdentifier: authorizationIdentifier;
authorizationIdentifier: identifier;
schemaCharacterSetSpecification: DEFAULT CHARACTER SET characterSetSpecification;
schemaElement: createTableStatement|createViewStatement;

createTableStatement: createTableHead (tableName? createTableExtraHead tableElementList createTableTail)?;
createTableHead: CREATE (OR REPLACE)? (GLOBAL|LOCAL)? (TEMPORARY|TEMP)? TABLE (IF NOT EXISTS)? ;// here orReplace is MariaDB-specific only
createTableExtraHead: (OF identifier)?;
tableElementList: LeftParen tableElement (Comma tableElement)* RightParen;
tableElement: (columnDefinition|tableConstraintDefinition) anyUnexpected??;
createTableTail: anyUnexpected;
//                 createTableTailForValues? createTableTailOther*
//                 createTableTailPartition? createTableTailOther*
//                 createTableTailOnCommit? createTableTailOther*;
//createTableTailPartition: PARTITION BY (RANGE|LIST|HASH|anyUnexpected) LeftParen createTableTailPartitionColumnSpec (Comma createTableTailPartitionColumnSpec)* RightParen;
//createTableTailPartitionColumnSpec: (columnName|valueExpression) anyWordsWithProperty?;
//createTableTailOnCommit: ON COMMIT (DELETE|PRESERVE) ROWS;
//createTableTailForValues: FOR VALUES createTablePartitionValuesSpec|DEFAULT;
//createTablePartitionValuesSpec: IN LeftParen queryExpression (Comma queryExpression)* RightParen |
//                     FROM LeftParen (queryExpression | MINVALUE | MAXVALUE) (Comma queryExpression | MINVALUE | MAXVALUE)* RightParen
//                       TO LeftParen (queryExpression | MINVALUE | MAXVALUE) (Comma queryExpression | MINVALUE | MAXVALUE)* RightParen |
//                     WITH LeftParen MODULUS signedNumericLiteral Comma REMAINDER signedNumericLiteral RightParen;
//createTableTailOther: ((~(LeftParen|RightParen|Period|Semicolon|Comma|
//        PARTITION|ON|FOR
//    ))|(LeftParen anyUnexpected* RightParen))+;

createViewStatement: CREATE VIEW tableName (LeftParen viewColumnList RightParen)? AS queryExpression (WITH (levelsClause)? CHECK OPTION)?;
viewColumnList: columnNameList;
levelsClause: (CASCADED|LOCAL);

// schema ddl
dropSchemaStatement: DROP SCHEMA schemaName dropBehaviour;
dropBehaviour: (CASCADE|RESTRICT);
alterTableStatement: ALTER anyWord* TABLE (IF EXISTS)? (tableName (alterTableAction (Comma alterTableAction)*)?)?;
alterTableAction: addColumnDefinition|alterColumnDefinition|renameColumnDefinition|dropColumnDefinition|addTableConstraintDefinition|dropTableConstraintDefinition|anyWordsWithProperty;
addColumnDefinition: ADD (COLUMN)? columnDefinition;
renameColumnDefinition: RENAME (COLUMN)? columnName TO identifier;
alterColumnDefinition: ALTER (COLUMN)? columnName alterColumnAction;
alterColumnAction: (setColumnDefaultClause|dropColumnDefaultClause);
setColumnDefaultClause: SET defaultClause;
dropColumnDefaultClause: DROP DEFAULT;
dropColumnDefinition: DROP (COLUMN)? columnName dropBehaviour;
addTableConstraintDefinition: ADD tableConstraintDefinition;
dropTableConstraintDefinition: DROP CONSTRAINT constraintName dropBehaviour;
dropTableStatement: DROP TABLE ifExistsSpec? (tableName (Comma tableName)*)? dropBehaviour?;
dropViewStatement: DROP VIEW ifExistsSpec?  (tableName (Comma tableName)*)? dropBehaviour?;
dropProcedureStatement: DROP PROCEDURE ifExistsSpec? qualifiedName dropBehaviour?;
dropCharacterSetStatement: DROP CHARACTER SET characterSetName;
ifExistsSpec: IF EXISTS ;

// data statements
selectStatementSingleRow: SELECT (setQuantifier)? selectList INTO selectTargetList tableExpression;
selectTargetList: parameterSpecification (Comma parameterSpecification)*;
deleteStatement: DELETE FROM tableName? ((AS)? correlationName)? whereClause?;
insertStatement: INSERT INTO (tableName insertColumnsAndSource?)?;
insertColumnsAndSource: (LeftParen (insertColumnList? | Asterisk) RightParen?)? (queryExpression | DEFAULT VALUES);
insertColumnList: columnNameList;

// UPDATE
updateStatement: UPDATE anyWordsWithProperty?? tableReference? (SET setClauseList? fromClause? whereClause? orderByClause? limitClause? anyWordsWithProperty??)?;
setClauseList: setClause (Comma setClause)*;
setClause: ((setTarget | setTargetList) (EqualsOperator updateSource)?)|anyUnexpected??;
setTarget: valueReference;
setTargetList: LeftParen valueReference? (Comma valueReference)* RightParen?;
updateSource: updateValue | (LeftParen updateValue (Comma updateValue)* RightParen?);
updateValue: valueExpression|DEFAULT;

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
anyWordsWithProperty2: (anyWord|IF)+ anyProperty overClause?; // to handle if as function like IF(c.tipo_cliente='F','Física','Jurídica')

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
    CURRENT_USER | SESSION_USER | SYSTEM_USER | USER | VALUE | RIGHT | LEFT |
    DATE | YEAR | MONTH | DAY | HOUR | MINUTE | SECOND | ZONE |
    ACTION | ADD | AUTHORIZATION | BY | CASCADE | CASCADED | CATALOG | COALESCE | COMMIT |
    CONSTRAINTS | CORRESPONDING | COUNT | DEFERRABLE | DEFERRED | IMMEDIATE |
    EXTRACT | FULL | GLOBAL | LOCAL | INDICATOR | INITIALLY | INTERVAL | ISOLATION | KEY | LEVEL |
    NAMES | NO | NULLIF| ONLY | OVERLAPS| PARTIAL | PRESERVE | READ | RESTRICT | ROLLBACK | SCHEMA |
    SESSION | SET | TEMPORARY | TIME | TIMESTAMP | TIMEZONE_HOUR | TIMEZONE_MINUTE | TRANSACTION |
    VIEW | WORK | WRITE | ARRAY | REPLACE
;
