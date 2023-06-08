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
package org.jkiss.dbeaver.model.stm;

import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;

public class STMKnownRuleNames {
    
    public static final String characterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSetSpecification];
    public static final String characterSetName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSetName];
    public static final String schemaName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaName];
    public static final String unqualifiedSchemaName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_unqualifiedSchemaName];
    public static final String catalogName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_catalogName];
    public static final String identifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_identifier];
    public static final String actualIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_actualIdentifier];

    public static final String dateString = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dateString];
    public static final String dateValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dateValue];
    public static final String yearsValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_yearsValue];
    public static final String datetimeValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeValue];
    public static final String monthsValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_monthsValue];
    public static final String daysValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_daysValue];
    public static final String timeString = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeString];
    public static final String timeValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeValue];
    public static final String hoursValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_hoursValue];
    public static final String minutesValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_minutesValue];
    public static final String secondsValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_secondsValue];
    public static final String secondsIntegerValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_secondsIntegerValue];
    public static final String secondsFraction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_secondsFraction];
    public static final String timeZoneInterval = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeZoneInterval];
    public static final String timestampString = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timestampString];
    public static final String intervalString = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalString];
    public static final String yearMonthLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_yearMonthLiteral];
    public static final String dayTimeLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dayTimeLiteral];
    public static final String dayTimeInterval = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dayTimeInterval];
    public static final String timeInterval = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeInterval];
    
    public static final String module = SQLStandardParser.ruleNames[SQLStandardParser.RULE_module];
    public static final String moduleNameClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_moduleNameClause];
    public static final String moduleName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_moduleName];
    public static final String moduleCharacterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_moduleCharacterSetSpecification];
    public static final String languageClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_languageClause];
    public static final String languageName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_languageName];
    public static final String moduleAuthorizationClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_moduleAuthorizationClause];
    public static final String moduleAuthorizationIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_moduleAuthorizationIdentifier];
    public static final String authorizationIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_authorizationIdentifier];
    
    public static final String dataType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dataType];
    public static final String characterStringType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterStringType];
    public static final String length = SQLStandardParser.ruleNames[SQLStandardParser.RULE_length];
    public static final String nationalCharacterStringType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nationalCharacterStringType];
    public static final String bitStringType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitStringType];
    public static final String numericType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numericType];
    public static final String exactNumericType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_exactNumericType];
    public static final String precision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_precision];
    public static final String scale = SQLStandardParser.ruleNames[SQLStandardParser.RULE_scale];
    public static final String approximateNumericType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_approximateNumericType];
    public static final String datetimeType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeType];
    public static final String timePrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timePrecision];
    public static final String timeFractionalSecondsPrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeFractionalSecondsPrecision];
    public static final String timestampPrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timestampPrecision];
    public static final String intervalType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalType];
    public static final String intervalQualifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalQualifier];
    public static final String startField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_startField];
    public static final String nonSecondDatetimeField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonSecondDatetimeField];
    public static final String intervalLeadingFieldPrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalLeadingFieldPrecision];
    public static final String endField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_endField];
    public static final String intervalFractionalSecondsPrecision = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalFractionalSecondsPrecision];
    public static final String singleDatetimeField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_singleDatetimeField];
    
    public static final String temporaryTableDeclaration = SQLStandardParser.ruleNames[SQLStandardParser.RULE_temporaryTableDeclaration];
    public static final String qualifiedLocalTableName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_qualifiedLocalTableName];
    public static final String localTableName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_localTableName];
    public static final String qualifiedIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_qualifiedIdentifier];
    public static final String tableElementList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableElementList];
    public static final String tableElement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableElement];
    
    public static final String columnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnDefinition];
    public static final String columnName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnName];
    
    public static final String domainName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_domainName];
    public static final String qualifiedName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_qualifiedName];
    
    public static final String defaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_defaultClause];
    public static final String defaultOption = SQLStandardParser.ruleNames[SQLStandardParser.RULE_defaultOption];
    
    public static final String literal = SQLStandardParser.ruleNames[SQLStandardParser.RULE_literal];
    public static final String unsignedNumericLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_unsignedNumericLiteral];
    public static final String signedNumericLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_signedNumericLiteral];
    public static final String characterStringLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterStringLiteral];
    public static final String generalLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_generalLiteral];
    public static final String datetimeLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeLiteral];
    public static final String dateLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dateLiteral];
    public static final String timeLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeLiteral];
    public static final String timestampLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timestampLiteral];
    public static final String intervalLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalLiteral];
    public static final String datetimeValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeValueFunction];
    public static final String currentDateValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_currentDateValueFunction];
    public static final String currentTimeValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_currentTimeValueFunction];
    public static final String currentTimestampValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_currentTimestampValueFunction];
    
    public static final String columnConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraintDefinition];
    public static final String constraintNameDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintNameDefinition];
    public static final String constraintName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintName];
    public static final String columnConstraint = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnConstraint];
    public static final String uniqueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_uniqueSpecification];
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
    public static final String checkConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_checkConstraintDefinition];
    public static final String searchCondition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_searchCondition];
    public static final String booleanTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanTerm];
    public static final String booleanFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanFactor];
    public static final String booleanTest = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanTest];
    public static final String booleanPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_booleanPrimary];
    public static final String predicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_predicate];
    public static final String comparisonPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_comparisonPredicate];
    public static final String rowValueConstructor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructor];
    public static final String rowValueConstructorElement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructorElement];
    public static final String valueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpression];
    public static final String numericValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numericValueExpression];
    public static final String term = SQLStandardParser.ruleNames[SQLStandardParser.RULE_term];
    public static final String factor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_factor];
    public static final String numericPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numericPrimary];
    public static final String valueExpressionPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueExpressionPrimary];
    public static final String unsignedValueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_unsignedValueSpecification];
    public static final String unsignedLiteral = SQLStandardParser.ruleNames[SQLStandardParser.RULE_unsignedLiteral];
    public static final String generalValueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_generalValueSpecification];
    public static final String parameterSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterSpecification];
    public static final String parameterName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterName];
    public static final String indicatorParameter = SQLStandardParser.ruleNames[SQLStandardParser.RULE_indicatorParameter];
    public static final String dynamicParameterSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicParameterSpecification];
    public static final String columnReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_columnReference];
    public static final String qualifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_qualifier];
    public static final String correlationName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correlationName];
    public static final String setFunctionSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setFunctionSpecification];
    public static final String generalSetFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_generalSetFunction];
    public static final String setFunctionType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setFunctionType];
    public static final String setQuantifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setQuantifier];
    
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
    public static final String selectList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectList];
    public static final String selectSublist = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectSublist];
    public static final String derivedColumn = SQLStandardParser.ruleNames[SQLStandardParser.RULE_derivedColumn];
    public static final String asClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_asClause];
    public static final String tableExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableExpression];
    public static final String queryPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryPrimary];
    public static final String queryTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryTerm];
    public static final String queryExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_queryExpression];
    
    public static final String fromClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_fromClause];
    public static final String nonjoinedTableReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nonjoinedTableReference];
    public static final String tableReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableReference];
    public static final String joinedTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinedTable];
    public static final String correlationSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correlationSpecification];
    public static final String derivedColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_derivedColumnList];
    public static final String derivedTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_derivedTable];
    public static final String tableSubquery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableSubquery];
    
    public static final String crossJoinTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_crossJoinTerm];
    public static final String naturalJoinTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_naturalJoinTerm];
    public static final String joinType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinType];
    public static final String outerJoinType = SQLStandardParser.ruleNames[SQLStandardParser.RULE_outerJoinType];
    public static final String joinSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinSpecification];
    public static final String joinCondition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinCondition];
    public static final String namedColumnsJoin = SQLStandardParser.ruleNames[SQLStandardParser.RULE_namedColumnsJoin];
    public static final String joinColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_joinColumnList];
    
    public static final String whereClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_whereClause];
    public static final String groupByClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_groupByClause];
    public static final String groupingColumnReferenceList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_groupingColumnReferenceList];
    public static final String groupingColumnReference = SQLStandardParser.ruleNames[SQLStandardParser.RULE_groupingColumnReference];
    public static final String collateClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_collateClause];
    public static final String collationName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_collationName];
    public static final String havingClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_havingClause];
    public static final String tableValueConstructor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableValueConstructor];
    public static final String tableValueConstructorList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableValueConstructorList];
    public static final String explicitTable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_explicitTable];
    public static final String correspondingSpec = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correspondingSpec];
    public static final String correspondingColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_correspondingColumnList];
    public static final String caseExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_caseExpression];
    public static final String caseAbbreviation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_caseAbbreviation];
    public static final String caseSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_caseSpecification];
    public static final String simpleCase = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleCase];
    public static final String caseOperand = SQLStandardParser.ruleNames[SQLStandardParser.RULE_caseOperand];
    public static final String simpleWhenClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleWhenClause];
    public static final String whenOperand = SQLStandardParser.ruleNames[SQLStandardParser.RULE_whenOperand];
    public static final String result = SQLStandardParser.ruleNames[SQLStandardParser.RULE_result];
    public static final String resultExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_resultExpression];
    public static final String elseClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_elseClause];
    public static final String searchedCase = SQLStandardParser.ruleNames[SQLStandardParser.RULE_searchedCase];
    public static final String searchedWhenClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_searchedWhenClause];
    public static final String castSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_castSpecification];
    public static final String castOperand = SQLStandardParser.ruleNames[SQLStandardParser.RULE_castOperand];
    public static final String castTarget = SQLStandardParser.ruleNames[SQLStandardParser.RULE_castTarget];
    public static final String numericValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numericValueFunction];
    public static final String positionExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_positionExpression];
    public static final String characterValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterValueExpression];
    public static final String concatenation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_concatenation];
    public static final String characterFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterFactor];
    public static final String characterPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterPrimary];
    
    public static final String stringValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_stringValueFunction];
    public static final String characterValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterValueFunction];
    public static final String characterSubstringFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSubstringFunction];
    public static final String startPosition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_startPosition];
    public static final String stringLength = SQLStandardParser.ruleNames[SQLStandardParser.RULE_stringLength];
    public static final String fold = SQLStandardParser.ruleNames[SQLStandardParser.RULE_fold];
    public static final String formOfUseConversion = SQLStandardParser.ruleNames[SQLStandardParser.RULE_formOfUseConversion];
    public static final String formOfUseConversionName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_formOfUseConversionName];
    public static final String characterTranslation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterTranslation];
    public static final String translationName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_translationName];
    public static final String trimFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_trimFunction];
    public static final String trimOperands = SQLStandardParser.ruleNames[SQLStandardParser.RULE_trimOperands];
    public static final String trimSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_trimSpecification];
    public static final String trimCharacter = SQLStandardParser.ruleNames[SQLStandardParser.RULE_trimCharacter];
    public static final String trimSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_trimSource];
    public static final String bitValueFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitValueFunction];
    public static final String bitSubstringFunction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitSubstringFunction];
    public static final String bitValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitValueExpression];
    public static final String bitConcatenation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitConcatenation];
    public static final String bitFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitFactor];
    public static final String bitPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitPrimary];
    public static final String extractExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractExpression];
    public static final String extractField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractField];
    public static final String datetimeField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeField];
    public static final String timeZoneField = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeZoneField];
    public static final String extractSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extractSource];
    public static final String datetimeValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeValueExpression];
    public static final String intervalTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalTerm];
    public static final String intervalFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalFactor];
    public static final String intervalPrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalPrimary];
    public static final String intervalValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_intervalValueExpression];
    public static final String datetimeTerm = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeTerm];
    public static final String datetimeFactor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimeFactor];
    public static final String datetimePrimary = SQLStandardParser.ruleNames[SQLStandardParser.RULE_datetimePrimary];
    public static final String timeZone = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeZone];
    public static final String timeZoneSpecifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_timeZoneSpecifier];
    public static final String lengthExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_lengthExpression];
    public static final String charLengthExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_charLengthExpression];
    public static final String stringValueExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_stringValueExpression];
    public static final String octetLengthExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_octetLengthExpression];
    public static final String bitLengthExpression = SQLStandardParser.ruleNames[SQLStandardParser.RULE_bitLengthExpression];
    public static final String nullSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nullSpecification];
    public static final String defaultSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_defaultSpecification];
    public static final String rowValueConstructorList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructorList];
    public static final String rowSubquery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowSubquery];
    public static final String compOp = SQLStandardParser.ruleNames[SQLStandardParser.RULE_compOp];
    public static final String betweenPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_betweenPredicate];
    public static final String inPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_inPredicate];
    public static final String inPredicateValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_inPredicateValue];
    public static final String inValueList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_inValueList];
    public static final String likePredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_likePredicate];
    public static final String matchValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_matchValue];
    public static final String pattern = SQLStandardParser.ruleNames[SQLStandardParser.RULE_pattern];
    public static final String escapeCharacter = SQLStandardParser.ruleNames[SQLStandardParser.RULE_escapeCharacter];
    public static final String nullPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_nullPredicate];
    public static final String quantifiedComparisonPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_quantifiedComparisonPredicate];
    public static final String quantifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_quantifier];
    public static final String all = SQLStandardParser.ruleNames[SQLStandardParser.RULE_all];
    public static final String some = SQLStandardParser.ruleNames[SQLStandardParser.RULE_some];
    public static final String existsPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_existsPredicate];
    public static final String matchPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_matchPredicate];
    public static final String overlapsPredicate = SQLStandardParser.ruleNames[SQLStandardParser.RULE_overlapsPredicate];
    public static final String rowValueConstructor1 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructor1];
    public static final String rowValueConstructor2 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rowValueConstructor2];
    public static final String truthValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_truthValue];
    
    public static final String constraintAttributes = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintAttributes];
    public static final String constraintCheckTime = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintCheckTime];
    public static final String tableConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableConstraintDefinition];
    public static final String tableConstraint = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableConstraint];
    public static final String uniqueConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_uniqueConstraintDefinition];
    public static final String uniqueColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_uniqueColumnList];
    public static final String referentialConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referentialConstraintDefinition];
    public static final String referencingColumns = SQLStandardParser.ruleNames[SQLStandardParser.RULE_referencingColumns];
    
    public static final String orderByClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_orderByClause];
    public static final String sortSpecificationList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sortSpecificationList];
    public static final String sortSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sortSpecification];
    public static final String sortKey = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sortKey];
    public static final String orderingSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_orderingSpecification];
    
    
    public static final String moduleContents = SQLStandardParser.ruleNames[SQLStandardParser.RULE_moduleContents];
    public static final String declareCursor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_declareCursor];
    public static final String cursorName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_cursorName];
    public static final String cursorSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_cursorSpecification];
    public static final String updatabilityClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updatabilityClause];
    public static final String dynamicDeclareCursor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicDeclareCursor];
    public static final String statementName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_statementName];
    
    public static final String procedure = SQLStandardParser.ruleNames[SQLStandardParser.RULE_procedure];
    public static final String procedureName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_procedureName];
    public static final String parameterDeclarationList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterDeclarationList];
    public static final String parameterDeclaration = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterDeclaration];
    public static final String statusParameter = SQLStandardParser.ruleNames[SQLStandardParser.RULE_statusParameter];
    public static final String sqlProcedureStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlProcedureStatement];
    
    public static final String sqlSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlSchemaStatement];
    public static final String sqlSchemaDefinitionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlSchemaDefinitionStatement];
    public static final String schemaDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaDefinition];
    public static final String schemaNameClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaNameClause];
    public static final String schemaAuthorizationIdentifier = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaAuthorizationIdentifier];
    public static final String schemaCharacterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaCharacterSetSpecification];
    public static final String schemaElement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaElement];
    
    public static final String domainDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_domainDefinition];
    public static final String domainConstraint = SQLStandardParser.ruleNames[SQLStandardParser.RULE_domainConstraint];
    public static final String tableDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_tableDefinition];
    public static final String viewDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_viewDefinition];
    public static final String viewColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_viewColumnList];
    public static final String levelsClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_levelsClause];
    
    public static final String grantStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_grantStatement];
    public static final String privileges = SQLStandardParser.ruleNames[SQLStandardParser.RULE_privileges];
    public static final String actionList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_actionList];
    public static final String action = SQLStandardParser.ruleNames[SQLStandardParser.RULE_action];
    public static final String privilegeColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_privilegeColumnList];
    public static final String objectName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_objectName];
    public static final String grantee = SQLStandardParser.ruleNames[SQLStandardParser.RULE_grantee];
    
    public static final String assertionDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_assertionDefinition];
    public static final String assertionCheck = SQLStandardParser.ruleNames[SQLStandardParser.RULE_assertionCheck];
    public static final String characterSetDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSetDefinition];
    public static final String characterSetSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_characterSetSource];
    public static final String existingCharacterSetName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_existingCharacterSetName];
    public static final String schemaCharacterSetName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_schemaCharacterSetName];
    
    public static final String limitedCollationDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_limitedCollationDefinition];
    public static final String collationSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_collationSource];
    public static final String collatingSequenceDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_collatingSequenceDefinition];
    public static final String externalCollation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_externalCollation];
    public static final String translationCollation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_translationCollation];
    public static final String collationDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_collationDefinition];
    public static final String padAttribute = SQLStandardParser.ruleNames[SQLStandardParser.RULE_padAttribute];
    public static final String translationDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_translationDefinition];
    public static final String sourceCharacterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sourceCharacterSetSpecification];
    public static final String targetCharacterSetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_targetCharacterSetSpecification];
    public static final String translationSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_translationSource];
    public static final String translationSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_translationSpecification];
    public static final String externalTranslation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_externalTranslation];
    
    public static final String sqlSchemaManipulationStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlSchemaManipulationStatement];
    public static final String dropSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropSchemaStatement];
    public static final String dropBehaviour = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropBehaviour];
    public static final String alterTableStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterTableStatement];
    public static final String alterTableAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterTableAction];
    public static final String addColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_addColumnDefinition];
    public static final String alterColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterColumnDefinition];
    public static final String alterColumnAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterColumnAction];
    public static final String setColumnDefaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setColumnDefaultClause];
    public static final String dropColumnDefaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropColumnDefaultClause];
    public static final String dropColumnDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropColumnDefinition];
    public static final String addTableConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_addTableConstraintDefinition];
    public static final String dropTableConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropTableConstraintDefinition];
    public static final String dropTableStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropTableStatement];
    public static final String dropViewStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropViewStatement];
    public static final String revokeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_revokeStatement];
    public static final String alterDomainStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterDomainStatement];
    public static final String alterDomainAction = SQLStandardParser.ruleNames[SQLStandardParser.RULE_alterDomainAction];
    public static final String setDomainDefaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setDomainDefaultClause];
    public static final String dropDomainDefaultClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropDomainDefaultClause];
    public static final String addDomainConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_addDomainConstraintDefinition];
    public static final String dropDomainConstraintDefinition = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropDomainConstraintDefinition];
    public static final String dropDomainStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropDomainStatement];
    public static final String dropCharacterSetStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropCharacterSetStatement];
    public static final String dropCollationStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropCollationStatement];
    public static final String dropTranslationStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropTranslationStatement];
    public static final String dropAssertionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dropAssertionStatement];
    
    public static final String sqlDataStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlDataStatement];
    public static final String openStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_openStatement];
    public static final String fetchStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_fetchStatement];
    public static final String fetchOrientation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_fetchOrientation];
    public static final String simpleValueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleValueSpecification];
    public static final String fetchTargetList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_fetchTargetList];
    public static final String targetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_targetSpecification];
    public static final String closeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_closeStatement];
    public static final String selectStatementSingleRow = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectStatementSingleRow];
    public static final String selectTargetList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectTargetList];
    
    public static final String sqlDataChangeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlDataChangeStatement];
    public static final String deleteStatementPositioned = SQLStandardParser.ruleNames[SQLStandardParser.RULE_deleteStatementPositioned];
    public static final String deleteStatementSearched = SQLStandardParser.ruleNames[SQLStandardParser.RULE_deleteStatementSearched];
    public static final String insertStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_insertStatement];
    public static final String insertColumnsAndSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_insertColumnsAndSource];
    public static final String insertColumnList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_insertColumnList];
    public static final String updateStatementPositioned = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateStatementPositioned];
    public static final String setClauseList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setClauseList];
    public static final String setClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setClause];
    public static final String objectColumn = SQLStandardParser.ruleNames[SQLStandardParser.RULE_objectColumn];
    public static final String updateSource = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateSource];
    public static final String updateStatementSearched = SQLStandardParser.ruleNames[SQLStandardParser.RULE_updateStatementSearched];
    
    public static final String sqlTransactionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlTransactionStatement];
    public static final String setTransactionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setTransactionStatement];
    public static final String transactionMode = SQLStandardParser.ruleNames[SQLStandardParser.RULE_transactionMode];
    public static final String isolationLevel = SQLStandardParser.ruleNames[SQLStandardParser.RULE_isolationLevel];
    public static final String levelOfIsolation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_levelOfIsolation];
    public static final String transactionAccessMode = SQLStandardParser.ruleNames[SQLStandardParser.RULE_transactionAccessMode];
    public static final String diagnosticsSize = SQLStandardParser.ruleNames[SQLStandardParser.RULE_diagnosticsSize];
    public static final String numberOfConditions = SQLStandardParser.ruleNames[SQLStandardParser.RULE_numberOfConditions];
    public static final String setConstraintsModeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setConstraintsModeStatement];
    public static final String constraintNameList = SQLStandardParser.ruleNames[SQLStandardParser.RULE_constraintNameList];
    public static final String commitStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_commitStatement];
    public static final String rollbackStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_rollbackStatement];
    
    public static final String sqlConnectionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlConnectionStatement];
    public static final String connectStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_connectStatement];
    public static final String connectionTarget = SQLStandardParser.ruleNames[SQLStandardParser.RULE_connectionTarget];
    public static final String sqlServerName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlServerName];
    public static final String connectionName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_connectionName];
    public static final String userName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_userName];
    public static final String setConnectionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setConnectionStatement];
    public static final String connectionObject = SQLStandardParser.ruleNames[SQLStandardParser.RULE_connectionObject];
    public static final String disconnectStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_disconnectStatement];
    public static final String disconnectObject = SQLStandardParser.ruleNames[SQLStandardParser.RULE_disconnectObject];
    
    public static final String sqlSessionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlSessionStatement];
    public static final String setCatalogStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setCatalogStatement];
    public static final String valueSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_valueSpecification];
    public static final String setSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setSchemaStatement];
    public static final String setNamesStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setNamesStatement];
    public static final String setSessionAuthorizationIdentifierStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setSessionAuthorizationIdentifierStatement];
    public static final String setLocalTimeZoneStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setLocalTimeZoneStatement];
    public static final String setTimeZoneValue = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setTimeZoneValue];
    public static final String sqlDynamicStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlDynamicStatement];
    public static final String systemDescriptorStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_systemDescriptorStatement];
    public static final String allocateDescriptorStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_allocateDescriptorStatement];
    public static final String descriptorName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_descriptorName];
    public static final String scopeOption = SQLStandardParser.ruleNames[SQLStandardParser.RULE_scopeOption];
    public static final String occurrences = SQLStandardParser.ruleNames[SQLStandardParser.RULE_occurrences];
    public static final String deallocateDescriptorStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_deallocateDescriptorStatement];
    
    public static final String setDescriptorStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setDescriptorStatement];
    public static final String setDescriptorInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setDescriptorInformation];
    public static final String setCount = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setCount];
    public static final String simpleValueSpecification1 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleValueSpecification1];
    public static final String itemNumber = SQLStandardParser.ruleNames[SQLStandardParser.RULE_itemNumber];
    public static final String setItemInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_setItemInformation];
    public static final String descriptorItemName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_descriptorItemName];
    public static final String simpleValueSpecification2 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleValueSpecification2];
    public static final String getDescriptorStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_getDescriptorStatement];
    public static final String getDescriptorInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_getDescriptorInformation];
    public static final String getCount = SQLStandardParser.ruleNames[SQLStandardParser.RULE_getCount];
    public static final String simpleTargetSpecification1 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleTargetSpecification1];
    public static final String simpleTargetSpecification = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleTargetSpecification];
    public static final String getItemInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_getItemInformation];
    public static final String simpleTargetSpecification2 = SQLStandardParser.ruleNames[SQLStandardParser.RULE_simpleTargetSpecification2];
    
    public static final String prepareStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_prepareStatement];
    public static final String sqlStatementName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlStatementName];
    public static final String extendedStatementName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extendedStatementName];
    public static final String sqlStatementVariable = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlStatementVariable];
    public static final String deallocatePreparedStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_deallocatePreparedStatement];
    public static final String describeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_describeStatement];
    public static final String describeInputStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_describeInputStatement];
    public static final String usingDescriptor = SQLStandardParser.ruleNames[SQLStandardParser.RULE_usingDescriptor];
    public static final String describeOutputStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_describeOutputStatement];
    
    public static final String executeStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_executeStatement];
    public static final String resultUsingClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_resultUsingClause];
    public static final String usingClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_usingClause];
    public static final String usingArguments = SQLStandardParser.ruleNames[SQLStandardParser.RULE_usingArguments];
    public static final String argument = SQLStandardParser.ruleNames[SQLStandardParser.RULE_argument];
    public static final String parameterUsingClause = SQLStandardParser.ruleNames[SQLStandardParser.RULE_parameterUsingClause];
    public static final String executeImmediateStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_executeImmediateStatement];
    
    public static final String sqlDynamicDataStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlDynamicDataStatement];
    public static final String allocateCursorStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_allocateCursorStatement];
    public static final String extendedCursorName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_extendedCursorName];
    public static final String dynamicOpenStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicOpenStatement];
    public static final String dynamicCursorName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicCursorName];
    public static final String dynamicCloseStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicCloseStatement];
    public static final String dynamicFetchStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicFetchStatement];
    public static final String dynamicDeleteStatementPositioned = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicDeleteStatementPositioned];
    public static final String dynamicUpdateStatementPositioned = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicUpdateStatementPositioned];
    
    public static final String sqlDiagnosticsStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlDiagnosticsStatement];
    public static final String getDiagnosticsStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_getDiagnosticsStatement];
    public static final String sqlDiagnosticsInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlDiagnosticsInformation];
    public static final String statementInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_statementInformation];
    public static final String statementInformationItem = SQLStandardParser.ruleNames[SQLStandardParser.RULE_statementInformationItem];
    public static final String statementInformationItemName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_statementInformationItemName];
    public static final String conditionInformation = SQLStandardParser.ruleNames[SQLStandardParser.RULE_conditionInformation];
    public static final String conditionNumber = SQLStandardParser.ruleNames[SQLStandardParser.RULE_conditionNumber];
    public static final String conditionInformationItem = SQLStandardParser.ruleNames[SQLStandardParser.RULE_conditionInformationItem];
    public static final String conditionInformationItemName = SQLStandardParser.ruleNames[SQLStandardParser.RULE_conditionInformationItemName];
    
    public static final String statementOrDeclaration = SQLStandardParser.ruleNames[SQLStandardParser.RULE_statementOrDeclaration];
    
    public static final String preparableStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableStatement];
    public static final String preparableSqlDataStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableSqlDataStatement];
    public static final String dynamicSingleRowSelectStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicSingleRowSelectStatement];
    public static final String dynamicSelectStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_dynamicSelectStatement];
    public static final String preparableDynamicDeleteStatementPositioned = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableDynamicDeleteStatementPositioned];
    public static final String preparableDynamicUpdateStatementPositioned = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableDynamicUpdateStatementPositioned];
    public static final String preparableSqlSchemaStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableSqlSchemaStatement];
    public static final String preparableSqlTransactionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableSqlTransactionStatement];
    public static final String preparableSqlSessionStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_preparableSqlSessionStatement];
    
    public static final String directSqlStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_directSqlStatement];
    public static final String directSqlDataStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_directSqlDataStatement];
    public static final String selectStatement = SQLStandardParser.ruleNames[SQLStandardParser.RULE_selectStatement];
    
    public static final String sqlQueries = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlQueries];
    public static final String sqlQuery = SQLStandardParser.ruleNames[SQLStandardParser.RULE_sqlQuery];
}
