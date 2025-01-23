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
package org.jkiss.dbeaver.model.stm;

import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;

public class STMKnownRuleNames {
    // root rule for script
    public static final String sqlQueries = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlQueries]; // EOF - don't stop early. must match all input
    public static final String sqlQuery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlQuery];

    public static final String directSqlDataStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_directSqlDataStatement];
    public static final String selectStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectStatement];

    // data type literals
    public static final String literal = SQLStandardParser.ruleNames[SQLStandardParser.RULE_literal];
    public static final String unsignedNumericLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_unsignedNumericLiteral];
    public static final String signedNumericLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_signedNumericLiteral];
    public static final String characterStringLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterStringLiteral];
    public static final String generalLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_generalLiteral];
    public static final String datetimeLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeLiteral];
    public static final String dateLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dateLiteral];
    public static final String timeLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeLiteral];
    public static final String timestampLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timestampLiteral];
    // intervalLiteral: INTERVAL (Sign)? StringLiteralContent intervalQualifier;
    public static final String intervalLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalLiteral];

    // identifiers
    public static final String characterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSetSpecification];
    public static final String characterSetName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSetName];
    public static final String schemaName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaName];
    public static final String qualifiedName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_qualifiedName];
    public static final String identifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_identifier];
    public static final String actualIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_actualIdentifier];

    // data types
    public static final String dataType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dataType];
    public static final String datetimeType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeType];
    public static final String intervalType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalType];
    public static final String intervalQualifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalQualifier];
    public static final String startField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_startField];
    public static final String nonSecondDatetimeField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonSecondDatetimeField];
    public static final String intervalLeadingFieldPrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalLeadingFieldPrecision];
    public static final String endField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_endField];
    public static final String intervalFractionalSecondsPrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalFractionalSecondsPrecision];
    public static final String singleDatetimeField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_singleDatetimeField];

    // column definition
    public static final String columnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnDefinition];
    public static final String columnName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnName];
    public static final String columnIndex = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnIndex];

    // default
    public static final String defaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_defaultClause];

    // column constraints
    public static final String columnConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraintDefinition];
    public static final String constraintNameDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintNameDefinition];
    public static final String constraintName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintName];
    public static final String columnConstraint = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraint];
    public static final String checkConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_checkConstraintDefinition];
    public static final String columnConstraintNotNull = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraintNotNull];
    public static final String columnConstraintUnique = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraintUnique];
    public static final String columnConstraintPrimaryKey = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraintPrimaryKey];

    // references
    public static final String referencesSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referencesSpecification];
    public static final String referencedTableAndColumns = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referencedTableAndColumns];
    public static final String tableName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableName];
    public static final String referenceColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referenceColumnList];
    public static final String columnNameList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnNameList];
    public static final String matchType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_matchType];
    public static final String referentialTriggeredAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referentialTriggeredAction];
    public static final String updateRule = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateRule];
    public static final String referentialAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referentialAction];
    public static final String deleteRule = SQLStandardParser.ruleNames[SQLStandardParser.RULE_deleteRule];

    // search conditions
    public static final String searchCondition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_searchCondition]; // (.*?) - for error recovery
    public static final String booleanTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanTerm];
    public static final String booleanFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanFactor];
    public static final String booleanTest = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanTest];
    public static final String booleanPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanPrimary];
    public static final String predicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_predicate];

    public static final String rowValuePredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValuePredicate];
    public static final String comparisonPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_comparisonPredicate];
    public static final String betweenPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_betweenPredicate];
    public static final String inPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_inPredicate];
    public static final String nullPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nullPredicate];
    public static final String quantifiedComparisonPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_quantifiedComparisonPredicate];
    public static final String matchPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_matchPredicate];
    public static final String overlapsPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_overlapsPredicate];

    public static final String compOp = SQLStandardParser.ruleNames[SQLStandardParser.RULE_compOp];
    public static final String quantifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_quantifier];
    public static final String truthValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_truthValue];
    public static final String existsPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_existsPredicate];
    public static final String likePredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_likePredicate];
    public static final String matchValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_matchValue];
    public static final String pattern = SQLStandardParser.ruleNames[SQLStandardParser.RULE_pattern];
    public static final String escapeCharacter = SQLStandardParser.ruleNames[SQLStandardParser.RULE_escapeCharacter];
    public static final String inPredicateValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_inPredicateValue];

    public static final String rowValueConstructor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructor];
    public static final String rowValueConstructorElement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructorElement];
    public static final String nullSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nullSpecification];
    public static final String defaultSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_defaultSpecification];
    public static final String rowValueConstructorList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructorList];
    public static final String rowSubquery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowSubquery];
    public static final String generalValueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_generalValueSpecification];
    public static final String parameterSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterSpecification];
    public static final String parameterName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterName];
    public static final String indicatorParameter = SQLStandardParser.ruleNames[SQLStandardParser.RULE_indicatorParameter];
    public static final String dynamicParameterSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicParameterSpecification];
    public static final String columnReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnReference];
    public static final String tupleRefSuffix = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tupleRefSuffix];
    public static final String valueReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueReference];

    public static final String valueRefNestedExpr = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueRefNestedExpr];
    public static final String valueRefIndexingStep = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueRefIndexingStep];
    public static final String valueRefIndexingStepDirect = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueRefIndexingStepDirect];
    public static final String valueRefIndexingStepSlice = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueRefIndexingStepSlice];

    public static final String valueExpressionCastSpec = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpressionCastSpec];
    public static final String variableExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_variableExpression];

    public static final String correlationName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correlationName];

    public static final String withClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_withClause];
    public static final String cteList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_cteList];
    public static final String with_list_element = SQLStandardParser.ruleNames[SQLStandardParser.RULE_with_list_element];
    public static final String queryName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryName];

    // select, subquery
    public static final String scalarSubquery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_scalarSubquery];
    public static final String subquery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_subquery];
    public static final String unionTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_unionTerm];
    public static final String exceptTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_exceptTerm];
    public static final String nonJoinQueryExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonJoinQueryExpression];
    public static final String nonJoinQueryTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonJoinQueryTerm];
    public static final String intersectTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intersectTerm];
    public static final String nonJoinQueryPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonJoinQueryPrimary];
    public static final String simpleTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleTable];
    public static final String querySpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_querySpecification];
    public static final String setQuantifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setQuantifier];
    public static final String selectList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectList]; // (Comma selectSublist)* contains any quantifier for error recovery;
    public static final String selectSublist = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectSublist]; // (.*?) for whole rule to handle select fields autocompletion when from immediately after select
    public static final String derivedColumn = SQLStandardParser.ruleNames[SQLStandardParser.RULE_derivedColumn];
    public static final String asClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_asClause];
    public static final String tableExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableExpression];
    public static final String queryPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryPrimary];
    public static final String queryTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryTerm];
    public static final String queryExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryExpression];

    // from
    public static final String fromClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_fromClause];
    public static final String nonjoinedTableReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonjoinedTableReference];
    public static final String tableReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableReference]; // '.*' to handle incomplete queries
    public static final String tableReferenceHints = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableReferenceHints]; // dialect-specific options, should be described and moved to dialects in future
    public static final String joinedTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinedTable];
    public static final String correlationSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correlationSpecification];
    public static final String derivedColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_derivedColumnList];
    public static final String derivedTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_derivedTable];
    public static final String tableSubquery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableSubquery];

    //joins
    public static final String crossJoinTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_crossJoinTerm];
    public static final String naturalJoinTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_naturalJoinTerm]; // (.*?) - for error recovery
    public static final String joinType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinType];
    public static final String outerJoinType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_outerJoinType];
    public static final String joinSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinSpecification];
    public static final String joinCondition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinCondition];
    public static final String namedColumnsJoin = SQLStandardParser.ruleNames[SQLStandardParser.RULE_namedColumnsJoin];
    public static final String joinColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinColumnList];

    // conditions
    public static final String whereClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_whereClause];
    public static final String groupByClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_groupByClause];
    public static final String groupingColumnReferenceList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_groupingColumnReferenceList];
    public static final String groupingColumnReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_groupingColumnReference];
    public static final String havingClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_havingClause];
    public static final String tableValueConstructor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableValueConstructor];
    public static final String explicitTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_explicitTable];
    public static final String correspondingSpec = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correspondingSpec];
    public static final String correspondingColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correspondingColumnList];
    public static final String caseExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_caseExpression];
    public static final String caseAbbreviation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_caseAbbreviation];
    public static final String simpleCase = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleCase];
    public static final String simpleWhenClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleWhenClause];
    public static final String result = SQLStandardParser.ruleNames[SQLStandardParser.RULE_result];
    public static final String elseClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_elseClause];
    public static final String searchedCase = SQLStandardParser.ruleNames[SQLStandardParser.RULE_searchedCase];
    public static final String searchedWhenClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_searchedWhenClause];
    public static final String castSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_castSpecification];
    public static final String castOperand = SQLStandardParser.ruleNames[SQLStandardParser.RULE_castOperand];

    // functions and operators
    //valueExpression: (numericValueExpression|characterValueExpression|datetimeValueExpression|intervalValueExpression);

    public static final String valueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpression];
    public static final String valueExpressionAtom = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpressionAtom];

    public static final String valueExpressionPrimarySignedBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpressionPrimarySignedBased];
    public static final String valueExpressionPrimaryBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpressionPrimaryBased];
    public static final String extractExpressionSignedBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractExpressionSignedBased];
    public static final String extractExpressionBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractExpressionBased];
    public static final String anyWordsWithPropertySignedBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyWordsWithPropertySignedBased];
    public static final String anyWordsWithPropertyBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyWordsWithPropertyBased];
    public static final String intervalExpressionBased = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalExpressionBased];

    public static final String concatenationOperation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_concatenationOperation];
    public static final String numericOperation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numericOperation];

    public static final String intervalOperation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalOperation];
    public static final String intervalOperation2 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalOperation2];

    public static final String valueExpressionPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpressionPrimary];

    public static final String numericPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numericPrimary];
    public static final String factor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_factor];
    public static final String term = SQLStandardParser.ruleNames[SQLStandardParser.RULE_term];

    public static final String characterPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterPrimary];
    public static final String characterValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterValueExpression];

    public static final String intervalPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalPrimary];
    public static final String intervalFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalFactor];
    public static final String intervalTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalTerm];
    public static final String intervalValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalValueExpression];

    public static final String datetimeValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeValueExpression];
    public static final String extractSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractSource];

    public static final String countAllExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_countAllExpression];
    public static final String extractExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractExpression];
    public static final String extractField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractField];
    public static final String datetimeField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeField];
    public static final String timeZoneField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeZoneField];

    // constraints
    public static final String constraintAttributes = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintAttributes];
    public static final String constraintCheckTime = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintCheckTime];
    public static final String tableConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableConstraintDefinition];
    public static final String tableConstraint = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableConstraint];
    public static final String uniqueConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_uniqueConstraintDefinition];
    public static final String uniqueColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_uniqueColumnList];
    public static final String referentialConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referentialConstraintDefinition];
    public static final String referencingColumns = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referencingColumns];

    // order by
    public static final String orderByClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_orderByClause];
    public static final String limitClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_limitClause];
    public static final String sortSpecificationList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sortSpecificationList];
    public static final String sortSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sortSpecification];
    public static final String sortKey = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sortKey];
    public static final String orderingSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_orderingSpecification];

    // schema definition
    public static final String sqlSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlSchemaStatement];
    public static final String schemaDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaDefinition];
    public static final String schemaNameClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaNameClause];
    public static final String schemaAuthorizationIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaAuthorizationIdentifier];
    public static final String authorizationIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_authorizationIdentifier];
    public static final String schemaCharacterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaCharacterSetSpecification];
    public static final String schemaElement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaElement];

    // table definition
    public static final String createTableStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_createTableStatement];
    public static final String createViewStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_createViewStatement];
    public static final String viewColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_viewColumnList];
    public static final String levelsClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_levelsClause];
    public static final String tableElementList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableElementList];
    public static final String tableElement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableElement];

    // schema ddl
    public static final String dropSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropSchemaStatement];
    public static final String dropBehaviour = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropBehaviour];
    public static final String alterTableStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterTableStatement];
    public static final String alterTableAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterTableAction];
    public static final String addColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_addColumnDefinition];
    public static final String alterColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterColumnDefinition];
    public static final String renameColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_renameColumnDefinition];
    public static final String alterColumnAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterColumnAction];
    public static final String setColumnDefaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setColumnDefaultClause];
    public static final String dropColumnDefaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropColumnDefaultClause];
    public static final String dropColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropColumnDefinition];
    public static final String addTableConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_addTableConstraintDefinition];
    public static final String dropTableConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropTableConstraintDefinition];
    public static final String dropTableStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropTableStatement];
    public static final String dropViewStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropViewStatement];
    public static final String dropCharacterSetStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropCharacterSetStatement];
    public static final String ifExistsSpec = SQLStandardParser.ruleNames[SQLStandardParser.RULE_ifExistsSpec];

    // data statements
    public static final String selectStatementSingleRow = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectStatementSingleRow];
    public static final String selectTargetList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectTargetList];
    public static final String selectTargetItem = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectTargetItem];
    public static final String deleteStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_deleteStatement];
    public static final String insertStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_insertStatement];
    public static final String insertColumnsAndSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_insertColumnsAndSource];
    public static final String insertColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_insertColumnList];
    public static final String updateStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateStatement];
    public static final String setClauseList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setClauseList];
    public static final String setClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setClause];
    public static final String setTarget = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setTarget];
    public static final String setTargetList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setTargetList];
    public static final String updateSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateSource];
    public static final String updateValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateValue];

    // transactions
    public static final String sqlTransactionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlTransactionStatement];
    public static final String setTransactionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setTransactionStatement];
    public static final String transactionMode = SQLStandardParser.ruleNames[SQLStandardParser.RULE_transactionMode];
    public static final String isolationLevel = SQLStandardParser.ruleNames[SQLStandardParser.RULE_isolationLevel];
    public static final String levelOfIsolation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_levelOfIsolation];
    public static final String transactionAccessMode = SQLStandardParser.ruleNames[SQLStandardParser.RULE_transactionAccessMode];
    public static final String setConstraintsModeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setConstraintsModeStatement];
    public static final String constraintNameList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintNameList];
    public static final String commitStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_commitStatement];
    public static final String rollbackStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rollbackStatement];

    // session
    public static final String sqlSessionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlSessionStatement];
    public static final String setCatalogStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setCatalogStatement];
    public static final String valueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueSpecification];
    public static final String setSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setSchemaStatement];
    public static final String setNamesStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setNamesStatement];
    public static final String setSessionAuthorizationIdentifierStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setSessionAuthorizationIdentifierStatement];
    public static final String setLocalTimeZoneStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setLocalTimeZoneStatement];
    public static final String setTimeZoneValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setTimeZoneValue];

    // unknown keyword, data type or function name
    public static final String anyWord = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyWord];
    public static final String anyValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyValue];
    public static final String anyWordWithAnyValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyWordWithAnyValue];
    public static final String anyProperty = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyProperty];
    public static final String anyWordsWithProperty = SQLStandardParser.ruleNames[SQLStandardParser.RULE_anyWordsWithProperty];

    public static final String tableHintKeywords = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableHintKeywords];

    public static final String nonReserved = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonReserved];

    public static final String RECURSIVE_TERM = SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.RECURSIVE);
    public static final String SET_TERM = SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.SET);
    public static final String SELECT_TERM = SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.SELECT);
    public static final String INTO_TERM = SQLStandardLexer.VOCABULARY.getSymbolicName(SQLStandardLexer.INTO);
    public static final String PERIOD_TERM = SQLStandardParser.VOCABULARY.getSymbolicName(SQLStandardParser.Period);
    public static final String RIGHT_PAREN_TERM = SQLStandardParser.VOCABULARY.getSymbolicName(SQLStandardParser.LeftParen);
    public static final String LEFT_PAREN_TERM = SQLStandardParser.VOCABULARY.getSymbolicName(SQLStandardParser.RightParen);
}
