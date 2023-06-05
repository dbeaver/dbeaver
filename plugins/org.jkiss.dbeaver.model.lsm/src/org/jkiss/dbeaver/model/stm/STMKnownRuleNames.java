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

import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.Sql92Parser;

public class STMKnownRuleNames {
    
    public static final String characterSetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_characterSetSpecification];
    public static final String characterSetName = Sql92Parser.ruleNames[Sql92Parser.RULE_characterSetName];
    public static final String schemaName = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaName];
    public static final String unqualifiedSchemaName = Sql92Parser.ruleNames[Sql92Parser.RULE_unqualifiedSchemaName];
    public static final String catalogName = Sql92Parser.ruleNames[Sql92Parser.RULE_catalogName];
    public static final String identifier = Sql92Parser.ruleNames[Sql92Parser.RULE_identifier];
    public static final String actualIdentifier = Sql92Parser.ruleNames[Sql92Parser.RULE_actualIdentifier];

    public static final String dateString = Sql92Parser.ruleNames[Sql92Parser.RULE_dateString];
    public static final String dateValue = Sql92Parser.ruleNames[Sql92Parser.RULE_dateValue];
    public static final String yearsValue = Sql92Parser.ruleNames[Sql92Parser.RULE_yearsValue];
    public static final String datetimeValue = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeValue];
    public static final String monthsValue = Sql92Parser.ruleNames[Sql92Parser.RULE_monthsValue];
    public static final String daysValue = Sql92Parser.ruleNames[Sql92Parser.RULE_daysValue];
    public static final String timeString = Sql92Parser.ruleNames[Sql92Parser.RULE_timeString];
    public static final String timeValue = Sql92Parser.ruleNames[Sql92Parser.RULE_timeValue];
    public static final String hoursValue = Sql92Parser.ruleNames[Sql92Parser.RULE_hoursValue];
    public static final String minutesValue = Sql92Parser.ruleNames[Sql92Parser.RULE_minutesValue];
    public static final String secondsValue = Sql92Parser.ruleNames[Sql92Parser.RULE_secondsValue];
    public static final String secondsIntegerValue = Sql92Parser.ruleNames[Sql92Parser.RULE_secondsIntegerValue];
    public static final String secondsFraction = Sql92Parser.ruleNames[Sql92Parser.RULE_secondsFraction];
    public static final String timeZoneInterval = Sql92Parser.ruleNames[Sql92Parser.RULE_timeZoneInterval];
    public static final String timestampString = Sql92Parser.ruleNames[Sql92Parser.RULE_timestampString];
    public static final String intervalString = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalString];
    public static final String yearMonthLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_yearMonthLiteral];
    public static final String dayTimeLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_dayTimeLiteral];
    public static final String dayTimeInterval = Sql92Parser.ruleNames[Sql92Parser.RULE_dayTimeInterval];
    public static final String timeInterval = Sql92Parser.ruleNames[Sql92Parser.RULE_timeInterval];
    
    public static final String module = Sql92Parser.ruleNames[Sql92Parser.RULE_module];
    public static final String moduleNameClause = Sql92Parser.ruleNames[Sql92Parser.RULE_moduleNameClause];
    public static final String moduleName = Sql92Parser.ruleNames[Sql92Parser.RULE_moduleName];
    public static final String moduleCharacterSetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_moduleCharacterSetSpecification];
    public static final String languageClause = Sql92Parser.ruleNames[Sql92Parser.RULE_languageClause];
    public static final String languageName = Sql92Parser.ruleNames[Sql92Parser.RULE_languageName];
    public static final String moduleAuthorizationClause = Sql92Parser.ruleNames[Sql92Parser.RULE_moduleAuthorizationClause];
    public static final String moduleAuthorizationIdentifier = Sql92Parser.ruleNames[Sql92Parser.RULE_moduleAuthorizationIdentifier];
    public static final String authorizationIdentifier = Sql92Parser.ruleNames[Sql92Parser.RULE_authorizationIdentifier];
    
    public static final String dataType = Sql92Parser.ruleNames[Sql92Parser.RULE_dataType];
    public static final String characterStringType = Sql92Parser.ruleNames[Sql92Parser.RULE_characterStringType];
    public static final String length = Sql92Parser.ruleNames[Sql92Parser.RULE_length];
    public static final String nationalCharacterStringType = Sql92Parser.ruleNames[Sql92Parser.RULE_nationalCharacterStringType];
    public static final String bitStringType = Sql92Parser.ruleNames[Sql92Parser.RULE_bitStringType];
    public static final String numericType = Sql92Parser.ruleNames[Sql92Parser.RULE_numericType];
    public static final String exactNumericType = Sql92Parser.ruleNames[Sql92Parser.RULE_exactNumericType];
    public static final String precision = Sql92Parser.ruleNames[Sql92Parser.RULE_precision];
    public static final String scale = Sql92Parser.ruleNames[Sql92Parser.RULE_scale];
    public static final String approximateNumericType = Sql92Parser.ruleNames[Sql92Parser.RULE_approximateNumericType];
    public static final String datetimeType = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeType];
    public static final String timePrecision = Sql92Parser.ruleNames[Sql92Parser.RULE_timePrecision];
    public static final String timeFractionalSecondsPrecision = Sql92Parser.ruleNames[Sql92Parser.RULE_timeFractionalSecondsPrecision];
    public static final String timestampPrecision = Sql92Parser.ruleNames[Sql92Parser.RULE_timestampPrecision];
    public static final String intervalType = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalType];
    public static final String intervalQualifier = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalQualifier];
    public static final String startField = Sql92Parser.ruleNames[Sql92Parser.RULE_startField];
    public static final String nonSecondDatetimeField = Sql92Parser.ruleNames[Sql92Parser.RULE_nonSecondDatetimeField];
    public static final String intervalLeadingFieldPrecision = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalLeadingFieldPrecision];
    public static final String endField = Sql92Parser.ruleNames[Sql92Parser.RULE_endField];
    public static final String intervalFractionalSecondsPrecision = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalFractionalSecondsPrecision];
    public static final String singleDatetimeField = Sql92Parser.ruleNames[Sql92Parser.RULE_singleDatetimeField];
    
    public static final String temporaryTableDeclaration = Sql92Parser.ruleNames[Sql92Parser.RULE_temporaryTableDeclaration];
    public static final String qualifiedLocalTableName = Sql92Parser.ruleNames[Sql92Parser.RULE_qualifiedLocalTableName];
    public static final String localTableName = Sql92Parser.ruleNames[Sql92Parser.RULE_localTableName];
    public static final String qualifiedIdentifier = Sql92Parser.ruleNames[Sql92Parser.RULE_qualifiedIdentifier];
    public static final String tableElementList = Sql92Parser.ruleNames[Sql92Parser.RULE_tableElementList];
    public static final String tableElement = Sql92Parser.ruleNames[Sql92Parser.RULE_tableElement];
    
    public static final String columnDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_columnDefinition];
    public static final String columnName = Sql92Parser.ruleNames[Sql92Parser.RULE_columnName];
    
    public static final String domainName = Sql92Parser.ruleNames[Sql92Parser.RULE_domainName];
    public static final String qualifiedName = Sql92Parser.ruleNames[Sql92Parser.RULE_qualifiedName];
    
    public static final String defaultClause = Sql92Parser.ruleNames[Sql92Parser.RULE_defaultClause];
    public static final String defaultOption = Sql92Parser.ruleNames[Sql92Parser.RULE_defaultOption];
    
    public static final String literal = Sql92Parser.ruleNames[Sql92Parser.RULE_literal];
    public static final String unsignedNumericLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_unsignedNumericLiteral];
    public static final String signedNumericLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_signedNumericLiteral];
    public static final String characterStringLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_characterStringLiteral];
    public static final String generalLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_generalLiteral];
    public static final String datetimeLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeLiteral];
    public static final String dateLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_dateLiteral];
    public static final String timeLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_timeLiteral];
    public static final String timestampLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_timestampLiteral];
    public static final String intervalLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalLiteral];
    public static final String datetimeValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeValueFunction];
    public static final String currentDateValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_currentDateValueFunction];
    public static final String currentTimeValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_currentTimeValueFunction];
    public static final String currentTimestampValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_currentTimestampValueFunction];
    
    public static final String columnConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_columnConstraintDefinition];
    public static final String constraintNameDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_constraintNameDefinition];
    public static final String constraintName = Sql92Parser.ruleNames[Sql92Parser.RULE_constraintName];
    public static final String columnConstraint = Sql92Parser.ruleNames[Sql92Parser.RULE_columnConstraint];
    public static final String uniqueSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_uniqueSpecification];
    public static final String referencesSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_referencesSpecification];
    public static final String referencedTableAndColumns = Sql92Parser.ruleNames[Sql92Parser.RULE_referencedTableAndColumns];
    public static final String tableName = Sql92Parser.ruleNames[Sql92Parser.RULE_tableName];
    public static final String referenceColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_referenceColumnList];
    public static final String columnNameList = Sql92Parser.ruleNames[Sql92Parser.RULE_columnNameList];
    public static final String matchType = Sql92Parser.ruleNames[Sql92Parser.RULE_matchType];
    public static final String referentialTriggeredAction = Sql92Parser.ruleNames[Sql92Parser.RULE_referentialTriggeredAction];
    public static final String updateRule = Sql92Parser.ruleNames[Sql92Parser.RULE_updateRule];
    public static final String referentialAction = Sql92Parser.ruleNames[Sql92Parser.RULE_referentialAction];
    public static final String deleteRule = Sql92Parser.ruleNames[Sql92Parser.RULE_deleteRule];
    public static final String checkConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_checkConstraintDefinition];
    public static final String searchCondition = Sql92Parser.ruleNames[Sql92Parser.RULE_searchCondition];
    public static final String booleanTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_booleanTerm];
    public static final String booleanFactor = Sql92Parser.ruleNames[Sql92Parser.RULE_booleanFactor];
    public static final String booleanTest = Sql92Parser.ruleNames[Sql92Parser.RULE_booleanTest];
    public static final String booleanPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_booleanPrimary];
    public static final String predicate = Sql92Parser.ruleNames[Sql92Parser.RULE_predicate];
    public static final String comparisonPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_comparisonPredicate];
    public static final String rowValueConstructor = Sql92Parser.ruleNames[Sql92Parser.RULE_rowValueConstructor];
    public static final String rowValueConstructorElement = Sql92Parser.ruleNames[Sql92Parser.RULE_rowValueConstructorElement];
    public static final String valueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_valueExpression];
    public static final String numericValueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_numericValueExpression];
    public static final String term = Sql92Parser.ruleNames[Sql92Parser.RULE_term];
    public static final String factor = Sql92Parser.ruleNames[Sql92Parser.RULE_factor];
    public static final String numericPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_numericPrimary];
    public static final String valueExpressionPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_valueExpressionPrimary];
    public static final String unsignedValueSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_unsignedValueSpecification];
    public static final String unsignedLiteral = Sql92Parser.ruleNames[Sql92Parser.RULE_unsignedLiteral];
    public static final String generalValueSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_generalValueSpecification];
    public static final String parameterSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_parameterSpecification];
    public static final String parameterName = Sql92Parser.ruleNames[Sql92Parser.RULE_parameterName];
    public static final String indicatorParameter = Sql92Parser.ruleNames[Sql92Parser.RULE_indicatorParameter];
    public static final String dynamicParameterSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicParameterSpecification];
    public static final String columnReference = Sql92Parser.ruleNames[Sql92Parser.RULE_columnReference];
    public static final String qualifier = Sql92Parser.ruleNames[Sql92Parser.RULE_qualifier];
    public static final String correlationName = Sql92Parser.ruleNames[Sql92Parser.RULE_correlationName];
    public static final String setFunctionSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_setFunctionSpecification];
    public static final String generalSetFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_generalSetFunction];
    public static final String setFunctionType = Sql92Parser.ruleNames[Sql92Parser.RULE_setFunctionType];
    public static final String setQuantifier = Sql92Parser.ruleNames[Sql92Parser.RULE_setQuantifier];
    
    public static final String scalarSubquery = Sql92Parser.ruleNames[Sql92Parser.RULE_scalarSubquery];
    public static final String subquery = Sql92Parser.ruleNames[Sql92Parser.RULE_subquery];
    public static final String unionTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_unionTerm];
    public static final String exceptTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_exceptTerm];
    public static final String nonJoinQueryExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_nonJoinQueryExpression];
    public static final String nonJoinQueryTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_nonJoinQueryTerm];
    public static final String intersectTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_intersectTerm];
    public static final String nonJoinQueryPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_nonJoinQueryPrimary];
    public static final String simpleTable = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleTable];
    public static final String querySpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_querySpecification];
    public static final String selectList = Sql92Parser.ruleNames[Sql92Parser.RULE_selectList];
    public static final String selectSublist = Sql92Parser.ruleNames[Sql92Parser.RULE_selectSublist];
    public static final String derivedColumn = Sql92Parser.ruleNames[Sql92Parser.RULE_derivedColumn];
    public static final String asClause = Sql92Parser.ruleNames[Sql92Parser.RULE_asClause];
    public static final String tableExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_tableExpression];
    public static final String queryPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_queryPrimary];
    public static final String queryTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_queryTerm];
    public static final String queryExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_queryExpression];
    
    public static final String fromClause = Sql92Parser.ruleNames[Sql92Parser.RULE_fromClause];
    public static final String nonjoinedTableReference = Sql92Parser.ruleNames[Sql92Parser.RULE_nonjoinedTableReference];
    public static final String tableReference = Sql92Parser.ruleNames[Sql92Parser.RULE_tableReference];
    public static final String joinedTable = Sql92Parser.ruleNames[Sql92Parser.RULE_joinedTable];
    public static final String correlationSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_correlationSpecification];
    public static final String derivedColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_derivedColumnList];
    public static final String derivedTable = Sql92Parser.ruleNames[Sql92Parser.RULE_derivedTable];
    public static final String tableSubquery = Sql92Parser.ruleNames[Sql92Parser.RULE_tableSubquery];
    
    public static final String crossJoinTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_crossJoinTerm];
    public static final String naturalJoinTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_naturalJoinTerm];
    public static final String joinType = Sql92Parser.ruleNames[Sql92Parser.RULE_joinType];
    public static final String outerJoinType = Sql92Parser.ruleNames[Sql92Parser.RULE_outerJoinType];
    public static final String joinSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_joinSpecification];
    public static final String joinCondition = Sql92Parser.ruleNames[Sql92Parser.RULE_joinCondition];
    public static final String namedColumnsJoin = Sql92Parser.ruleNames[Sql92Parser.RULE_namedColumnsJoin];
    public static final String joinColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_joinColumnList];
    
    public static final String whereClause = Sql92Parser.ruleNames[Sql92Parser.RULE_whereClause];
    public static final String groupByClause = Sql92Parser.ruleNames[Sql92Parser.RULE_groupByClause];
    public static final String groupingColumnReferenceList = Sql92Parser.ruleNames[Sql92Parser.RULE_groupingColumnReferenceList];
    public static final String groupingColumnReference = Sql92Parser.ruleNames[Sql92Parser.RULE_groupingColumnReference];
    public static final String collateClause = Sql92Parser.ruleNames[Sql92Parser.RULE_collateClause];
    public static final String collationName = Sql92Parser.ruleNames[Sql92Parser.RULE_collationName];
    public static final String havingClause = Sql92Parser.ruleNames[Sql92Parser.RULE_havingClause];
    public static final String tableValueConstructor = Sql92Parser.ruleNames[Sql92Parser.RULE_tableValueConstructor];
    public static final String tableValueConstructorList = Sql92Parser.ruleNames[Sql92Parser.RULE_tableValueConstructorList];
    public static final String explicitTable = Sql92Parser.ruleNames[Sql92Parser.RULE_explicitTable];
    public static final String correspondingSpec = Sql92Parser.ruleNames[Sql92Parser.RULE_correspondingSpec];
    public static final String correspondingColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_correspondingColumnList];
    public static final String caseExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_caseExpression];
    public static final String caseAbbreviation = Sql92Parser.ruleNames[Sql92Parser.RULE_caseAbbreviation];
    public static final String caseSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_caseSpecification];
    public static final String simpleCase = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleCase];
    public static final String caseOperand = Sql92Parser.ruleNames[Sql92Parser.RULE_caseOperand];
    public static final String simpleWhenClause = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleWhenClause];
    public static final String whenOperand = Sql92Parser.ruleNames[Sql92Parser.RULE_whenOperand];
    public static final String result = Sql92Parser.ruleNames[Sql92Parser.RULE_result];
    public static final String resultExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_resultExpression];
    public static final String elseClause = Sql92Parser.ruleNames[Sql92Parser.RULE_elseClause];
    public static final String searchedCase = Sql92Parser.ruleNames[Sql92Parser.RULE_searchedCase];
    public static final String searchedWhenClause = Sql92Parser.ruleNames[Sql92Parser.RULE_searchedWhenClause];
    public static final String castSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_castSpecification];
    public static final String castOperand = Sql92Parser.ruleNames[Sql92Parser.RULE_castOperand];
    public static final String castTarget = Sql92Parser.ruleNames[Sql92Parser.RULE_castTarget];
    public static final String numericValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_numericValueFunction];
    public static final String positionExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_positionExpression];
    public static final String characterValueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_characterValueExpression];
    public static final String concatenation = Sql92Parser.ruleNames[Sql92Parser.RULE_concatenation];
    public static final String characterFactor = Sql92Parser.ruleNames[Sql92Parser.RULE_characterFactor];
    public static final String characterPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_characterPrimary];
    
    public static final String stringValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_stringValueFunction];
    public static final String characterValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_characterValueFunction];
    public static final String characterSubstringFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_characterSubstringFunction];
    public static final String startPosition = Sql92Parser.ruleNames[Sql92Parser.RULE_startPosition];
    public static final String stringLength = Sql92Parser.ruleNames[Sql92Parser.RULE_stringLength];
    public static final String fold = Sql92Parser.ruleNames[Sql92Parser.RULE_fold];
    public static final String formOfUseConversion = Sql92Parser.ruleNames[Sql92Parser.RULE_formOfUseConversion];
    public static final String formOfUseConversionName = Sql92Parser.ruleNames[Sql92Parser.RULE_formOfUseConversionName];
    public static final String characterTranslation = Sql92Parser.ruleNames[Sql92Parser.RULE_characterTranslation];
    public static final String translationName = Sql92Parser.ruleNames[Sql92Parser.RULE_translationName];
    public static final String trimFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_trimFunction];
    public static final String trimOperands = Sql92Parser.ruleNames[Sql92Parser.RULE_trimOperands];
    public static final String trimSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_trimSpecification];
    public static final String trimCharacter = Sql92Parser.ruleNames[Sql92Parser.RULE_trimCharacter];
    public static final String trimSource = Sql92Parser.ruleNames[Sql92Parser.RULE_trimSource];
    public static final String bitValueFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_bitValueFunction];
    public static final String bitSubstringFunction = Sql92Parser.ruleNames[Sql92Parser.RULE_bitSubstringFunction];
    public static final String bitValueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_bitValueExpression];
    public static final String bitConcatenation = Sql92Parser.ruleNames[Sql92Parser.RULE_bitConcatenation];
    public static final String bitFactor = Sql92Parser.ruleNames[Sql92Parser.RULE_bitFactor];
    public static final String bitPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_bitPrimary];
    public static final String extractExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_extractExpression];
    public static final String extractField = Sql92Parser.ruleNames[Sql92Parser.RULE_extractField];
    public static final String datetimeField = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeField];
    public static final String timeZoneField = Sql92Parser.ruleNames[Sql92Parser.RULE_timeZoneField];
    public static final String extractSource = Sql92Parser.ruleNames[Sql92Parser.RULE_extractSource];
    public static final String datetimeValueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeValueExpression];
    public static final String intervalTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalTerm];
    public static final String intervalFactor = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalFactor];
    public static final String intervalPrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalPrimary];
    public static final String intervalValueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_intervalValueExpression];
    public static final String datetimeTerm = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeTerm];
    public static final String datetimeFactor = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimeFactor];
    public static final String datetimePrimary = Sql92Parser.ruleNames[Sql92Parser.RULE_datetimePrimary];
    public static final String timeZone = Sql92Parser.ruleNames[Sql92Parser.RULE_timeZone];
    public static final String timeZoneSpecifier = Sql92Parser.ruleNames[Sql92Parser.RULE_timeZoneSpecifier];
    public static final String lengthExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_lengthExpression];
    public static final String charLengthExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_charLengthExpression];
    public static final String stringValueExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_stringValueExpression];
    public static final String octetLengthExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_octetLengthExpression];
    public static final String bitLengthExpression = Sql92Parser.ruleNames[Sql92Parser.RULE_bitLengthExpression];
    public static final String nullSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_nullSpecification];
    public static final String defaultSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_defaultSpecification];
    public static final String rowValueConstructorList = Sql92Parser.ruleNames[Sql92Parser.RULE_rowValueConstructorList];
    public static final String rowSubquery = Sql92Parser.ruleNames[Sql92Parser.RULE_rowSubquery];
    public static final String compOp = Sql92Parser.ruleNames[Sql92Parser.RULE_compOp];
    public static final String betweenPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_betweenPredicate];
    public static final String inPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_inPredicate];
    public static final String inPredicateValue = Sql92Parser.ruleNames[Sql92Parser.RULE_inPredicateValue];
    public static final String inValueList = Sql92Parser.ruleNames[Sql92Parser.RULE_inValueList];
    public static final String likePredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_likePredicate];
    public static final String matchValue = Sql92Parser.ruleNames[Sql92Parser.RULE_matchValue];
    public static final String pattern = Sql92Parser.ruleNames[Sql92Parser.RULE_pattern];
    public static final String escapeCharacter = Sql92Parser.ruleNames[Sql92Parser.RULE_escapeCharacter];
    public static final String nullPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_nullPredicate];
    public static final String quantifiedComparisonPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_quantifiedComparisonPredicate];
    public static final String quantifier = Sql92Parser.ruleNames[Sql92Parser.RULE_quantifier];
    public static final String all = Sql92Parser.ruleNames[Sql92Parser.RULE_all];
    public static final String some = Sql92Parser.ruleNames[Sql92Parser.RULE_some];
    public static final String existsPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_existsPredicate];
    public static final String matchPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_matchPredicate];
    public static final String overlapsPredicate = Sql92Parser.ruleNames[Sql92Parser.RULE_overlapsPredicate];
    public static final String rowValueConstructor1 = Sql92Parser.ruleNames[Sql92Parser.RULE_rowValueConstructor1];
    public static final String rowValueConstructor2 = Sql92Parser.ruleNames[Sql92Parser.RULE_rowValueConstructor2];
    public static final String truthValue = Sql92Parser.ruleNames[Sql92Parser.RULE_truthValue];
    
    public static final String constraintAttributes = Sql92Parser.ruleNames[Sql92Parser.RULE_constraintAttributes];
    public static final String constraintCheckTime = Sql92Parser.ruleNames[Sql92Parser.RULE_constraintCheckTime];
    public static final String tableConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_tableConstraintDefinition];
    public static final String tableConstraint = Sql92Parser.ruleNames[Sql92Parser.RULE_tableConstraint];
    public static final String uniqueConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_uniqueConstraintDefinition];
    public static final String uniqueColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_uniqueColumnList];
    public static final String referentialConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_referentialConstraintDefinition];
    public static final String referencingColumns = Sql92Parser.ruleNames[Sql92Parser.RULE_referencingColumns];
    
    public static final String orderByClause = Sql92Parser.ruleNames[Sql92Parser.RULE_orderByClause];
    public static final String sortSpecificationList = Sql92Parser.ruleNames[Sql92Parser.RULE_sortSpecificationList];
    public static final String sortSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_sortSpecification];
    public static final String sortKey = Sql92Parser.ruleNames[Sql92Parser.RULE_sortKey];
    public static final String orderingSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_orderingSpecification];
    
    
    public static final String moduleContents = Sql92Parser.ruleNames[Sql92Parser.RULE_moduleContents];
    public static final String declareCursor = Sql92Parser.ruleNames[Sql92Parser.RULE_declareCursor];
    public static final String cursorName = Sql92Parser.ruleNames[Sql92Parser.RULE_cursorName];
    public static final String cursorSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_cursorSpecification];
    public static final String updatabilityClause = Sql92Parser.ruleNames[Sql92Parser.RULE_updatabilityClause];
    public static final String dynamicDeclareCursor = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicDeclareCursor];
    public static final String statementName = Sql92Parser.ruleNames[Sql92Parser.RULE_statementName];
    
    public static final String procedure = Sql92Parser.ruleNames[Sql92Parser.RULE_procedure];
    public static final String procedureName = Sql92Parser.ruleNames[Sql92Parser.RULE_procedureName];
    public static final String parameterDeclarationList = Sql92Parser.ruleNames[Sql92Parser.RULE_parameterDeclarationList];
    public static final String parameterDeclaration = Sql92Parser.ruleNames[Sql92Parser.RULE_parameterDeclaration];
    public static final String statusParameter = Sql92Parser.ruleNames[Sql92Parser.RULE_statusParameter];
    public static final String sqlProcedureStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlProcedureStatement];
    
    public static final String sqlSchemaStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlSchemaStatement];
    public static final String sqlSchemaDefinitionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlSchemaDefinitionStatement];
    public static final String schemaDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaDefinition];
    public static final String schemaNameClause = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaNameClause];
    public static final String schemaAuthorizationIdentifier = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaAuthorizationIdentifier];
    public static final String schemaCharacterSetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaCharacterSetSpecification];
    public static final String schemaElement = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaElement];
    
    public static final String domainDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_domainDefinition];
    public static final String domainConstraint = Sql92Parser.ruleNames[Sql92Parser.RULE_domainConstraint];
    public static final String tableDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_tableDefinition];
    public static final String viewDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_viewDefinition];
    public static final String viewColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_viewColumnList];
    public static final String levelsClause = Sql92Parser.ruleNames[Sql92Parser.RULE_levelsClause];
    
    public static final String grantStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_grantStatement];
    public static final String privileges = Sql92Parser.ruleNames[Sql92Parser.RULE_privileges];
    public static final String actionList = Sql92Parser.ruleNames[Sql92Parser.RULE_actionList];
    public static final String action = Sql92Parser.ruleNames[Sql92Parser.RULE_action];
    public static final String privilegeColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_privilegeColumnList];
    public static final String objectName = Sql92Parser.ruleNames[Sql92Parser.RULE_objectName];
    public static final String grantee = Sql92Parser.ruleNames[Sql92Parser.RULE_grantee];
    
    public static final String assertionDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_assertionDefinition];
    public static final String assertionCheck = Sql92Parser.ruleNames[Sql92Parser.RULE_assertionCheck];
    public static final String characterSetDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_characterSetDefinition];
    public static final String characterSetSource = Sql92Parser.ruleNames[Sql92Parser.RULE_characterSetSource];
    public static final String existingCharacterSetName = Sql92Parser.ruleNames[Sql92Parser.RULE_existingCharacterSetName];
    public static final String schemaCharacterSetName = Sql92Parser.ruleNames[Sql92Parser.RULE_schemaCharacterSetName];
    
    public static final String limitedCollationDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_limitedCollationDefinition];
    public static final String collationSource = Sql92Parser.ruleNames[Sql92Parser.RULE_collationSource];
    public static final String collatingSequenceDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_collatingSequenceDefinition];
    public static final String externalCollation = Sql92Parser.ruleNames[Sql92Parser.RULE_externalCollation];
    public static final String translationCollation = Sql92Parser.ruleNames[Sql92Parser.RULE_translationCollation];
    public static final String collationDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_collationDefinition];
    public static final String padAttribute = Sql92Parser.ruleNames[Sql92Parser.RULE_padAttribute];
    public static final String translationDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_translationDefinition];
    public static final String sourceCharacterSetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_sourceCharacterSetSpecification];
    public static final String targetCharacterSetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_targetCharacterSetSpecification];
    public static final String translationSource = Sql92Parser.ruleNames[Sql92Parser.RULE_translationSource];
    public static final String translationSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_translationSpecification];
    public static final String externalTranslation = Sql92Parser.ruleNames[Sql92Parser.RULE_externalTranslation];
    
    public static final String sqlSchemaManipulationStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlSchemaManipulationStatement];
    public static final String dropSchemaStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropSchemaStatement];
    public static final String dropBehaviour = Sql92Parser.ruleNames[Sql92Parser.RULE_dropBehaviour];
    public static final String alterTableStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_alterTableStatement];
    public static final String alterTableAction = Sql92Parser.ruleNames[Sql92Parser.RULE_alterTableAction];
    public static final String addColumnDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_addColumnDefinition];
    public static final String alterColumnDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_alterColumnDefinition];
    public static final String alterColumnAction = Sql92Parser.ruleNames[Sql92Parser.RULE_alterColumnAction];
    public static final String setColumnDefaultClause = Sql92Parser.ruleNames[Sql92Parser.RULE_setColumnDefaultClause];
    public static final String dropColumnDefaultClause = Sql92Parser.ruleNames[Sql92Parser.RULE_dropColumnDefaultClause];
    public static final String dropColumnDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_dropColumnDefinition];
    public static final String addTableConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_addTableConstraintDefinition];
    public static final String dropTableConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_dropTableConstraintDefinition];
    public static final String dropTableStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropTableStatement];
    public static final String dropViewStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropViewStatement];
    public static final String revokeStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_revokeStatement];
    public static final String alterDomainStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_alterDomainStatement];
    public static final String alterDomainAction = Sql92Parser.ruleNames[Sql92Parser.RULE_alterDomainAction];
    public static final String setDomainDefaultClause = Sql92Parser.ruleNames[Sql92Parser.RULE_setDomainDefaultClause];
    public static final String dropDomainDefaultClause = Sql92Parser.ruleNames[Sql92Parser.RULE_dropDomainDefaultClause];
    public static final String addDomainConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_addDomainConstraintDefinition];
    public static final String dropDomainConstraintDefinition = Sql92Parser.ruleNames[Sql92Parser.RULE_dropDomainConstraintDefinition];
    public static final String dropDomainStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropDomainStatement];
    public static final String dropCharacterSetStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropCharacterSetStatement];
    public static final String dropCollationStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropCollationStatement];
    public static final String dropTranslationStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropTranslationStatement];
    public static final String dropAssertionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dropAssertionStatement];
    
    public static final String sqlDataStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlDataStatement];
    public static final String openStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_openStatement];
    public static final String fetchStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_fetchStatement];
    public static final String fetchOrientation = Sql92Parser.ruleNames[Sql92Parser.RULE_fetchOrientation];
    public static final String simpleValueSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleValueSpecification];
    public static final String fetchTargetList = Sql92Parser.ruleNames[Sql92Parser.RULE_fetchTargetList];
    public static final String targetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_targetSpecification];
    public static final String closeStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_closeStatement];
    public static final String selectStatementSingleRow = Sql92Parser.ruleNames[Sql92Parser.RULE_selectStatementSingleRow];
    public static final String selectTargetList = Sql92Parser.ruleNames[Sql92Parser.RULE_selectTargetList];
    
    public static final String sqlDataChangeStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlDataChangeStatement];
    public static final String deleteStatementPositioned = Sql92Parser.ruleNames[Sql92Parser.RULE_deleteStatementPositioned];
    public static final String deleteStatementSearched = Sql92Parser.ruleNames[Sql92Parser.RULE_deleteStatementSearched];
    public static final String insertStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_insertStatement];
    public static final String insertColumnsAndSource = Sql92Parser.ruleNames[Sql92Parser.RULE_insertColumnsAndSource];
    public static final String insertColumnList = Sql92Parser.ruleNames[Sql92Parser.RULE_insertColumnList];
    public static final String updateStatementPositioned = Sql92Parser.ruleNames[Sql92Parser.RULE_updateStatementPositioned];
    public static final String setClauseList = Sql92Parser.ruleNames[Sql92Parser.RULE_setClauseList];
    public static final String setClause = Sql92Parser.ruleNames[Sql92Parser.RULE_setClause];
    public static final String objectColumn = Sql92Parser.ruleNames[Sql92Parser.RULE_objectColumn];
    public static final String updateSource = Sql92Parser.ruleNames[Sql92Parser.RULE_updateSource];
    public static final String updateStatementSearched = Sql92Parser.ruleNames[Sql92Parser.RULE_updateStatementSearched];
    
    public static final String sqlTransactionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlTransactionStatement];
    public static final String setTransactionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setTransactionStatement];
    public static final String transactionMode = Sql92Parser.ruleNames[Sql92Parser.RULE_transactionMode];
    public static final String isolationLevel = Sql92Parser.ruleNames[Sql92Parser.RULE_isolationLevel];
    public static final String levelOfIsolation = Sql92Parser.ruleNames[Sql92Parser.RULE_levelOfIsolation];
    public static final String transactionAccessMode = Sql92Parser.ruleNames[Sql92Parser.RULE_transactionAccessMode];
    public static final String diagnosticsSize = Sql92Parser.ruleNames[Sql92Parser.RULE_diagnosticsSize];
    public static final String numberOfConditions = Sql92Parser.ruleNames[Sql92Parser.RULE_numberOfConditions];
    public static final String setConstraintsModeStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setConstraintsModeStatement];
    public static final String constraintNameList = Sql92Parser.ruleNames[Sql92Parser.RULE_constraintNameList];
    public static final String commitStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_commitStatement];
    public static final String rollbackStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_rollbackStatement];
    
    public static final String sqlConnectionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlConnectionStatement];
    public static final String connectStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_connectStatement];
    public static final String connectionTarget = Sql92Parser.ruleNames[Sql92Parser.RULE_connectionTarget];
    public static final String sqlServerName = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlServerName];
    public static final String connectionName = Sql92Parser.ruleNames[Sql92Parser.RULE_connectionName];
    public static final String userName = Sql92Parser.ruleNames[Sql92Parser.RULE_userName];
    public static final String setConnectionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setConnectionStatement];
    public static final String connectionObject = Sql92Parser.ruleNames[Sql92Parser.RULE_connectionObject];
    public static final String disconnectStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_disconnectStatement];
    public static final String disconnectObject = Sql92Parser.ruleNames[Sql92Parser.RULE_disconnectObject];
    
    public static final String sqlSessionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlSessionStatement];
    public static final String setCatalogStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setCatalogStatement];
    public static final String valueSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_valueSpecification];
    public static final String setSchemaStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setSchemaStatement];
    public static final String setNamesStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setNamesStatement];
    public static final String setSessionAuthorizationIdentifierStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setSessionAuthorizationIdentifierStatement];
    public static final String setLocalTimeZoneStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setLocalTimeZoneStatement];
    public static final String setTimeZoneValue = Sql92Parser.ruleNames[Sql92Parser.RULE_setTimeZoneValue];
    public static final String sqlDynamicStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlDynamicStatement];
    public static final String systemDescriptorStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_systemDescriptorStatement];
    public static final String allocateDescriptorStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_allocateDescriptorStatement];
    public static final String descriptorName = Sql92Parser.ruleNames[Sql92Parser.RULE_descriptorName];
    public static final String scopeOption = Sql92Parser.ruleNames[Sql92Parser.RULE_scopeOption];
    public static final String occurrences = Sql92Parser.ruleNames[Sql92Parser.RULE_occurrences];
    public static final String deallocateDescriptorStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_deallocateDescriptorStatement];
    
    public static final String setDescriptorStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_setDescriptorStatement];
    public static final String setDescriptorInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_setDescriptorInformation];
    public static final String setCount = Sql92Parser.ruleNames[Sql92Parser.RULE_setCount];
    public static final String simpleValueSpecification1 = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleValueSpecification1];
    public static final String itemNumber = Sql92Parser.ruleNames[Sql92Parser.RULE_itemNumber];
    public static final String setItemInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_setItemInformation];
    public static final String descriptorItemName = Sql92Parser.ruleNames[Sql92Parser.RULE_descriptorItemName];
    public static final String simpleValueSpecification2 = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleValueSpecification2];
    public static final String getDescriptorStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_getDescriptorStatement];
    public static final String getDescriptorInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_getDescriptorInformation];
    public static final String getCount = Sql92Parser.ruleNames[Sql92Parser.RULE_getCount];
    public static final String simpleTargetSpecification1 = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleTargetSpecification1];
    public static final String simpleTargetSpecification = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleTargetSpecification];
    public static final String getItemInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_getItemInformation];
    public static final String simpleTargetSpecification2 = Sql92Parser.ruleNames[Sql92Parser.RULE_simpleTargetSpecification2];
    
    public static final String prepareStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_prepareStatement];
    public static final String sqlStatementName = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlStatementName];
    public static final String extendedStatementName = Sql92Parser.ruleNames[Sql92Parser.RULE_extendedStatementName];
    public static final String sqlStatementVariable = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlStatementVariable];
    public static final String deallocatePreparedStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_deallocatePreparedStatement];
    public static final String describeStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_describeStatement];
    public static final String describeInputStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_describeInputStatement];
    public static final String usingDescriptor = Sql92Parser.ruleNames[Sql92Parser.RULE_usingDescriptor];
    public static final String describeOutputStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_describeOutputStatement];
    
    public static final String executeStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_executeStatement];
    public static final String resultUsingClause = Sql92Parser.ruleNames[Sql92Parser.RULE_resultUsingClause];
    public static final String usingClause = Sql92Parser.ruleNames[Sql92Parser.RULE_usingClause];
    public static final String usingArguments = Sql92Parser.ruleNames[Sql92Parser.RULE_usingArguments];
    public static final String argument = Sql92Parser.ruleNames[Sql92Parser.RULE_argument];
    public static final String parameterUsingClause = Sql92Parser.ruleNames[Sql92Parser.RULE_parameterUsingClause];
    public static final String executeImmediateStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_executeImmediateStatement];
    
    public static final String sqlDynamicDataStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlDynamicDataStatement];
    public static final String allocateCursorStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_allocateCursorStatement];
    public static final String extendedCursorName = Sql92Parser.ruleNames[Sql92Parser.RULE_extendedCursorName];
    public static final String dynamicOpenStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicOpenStatement];
    public static final String dynamicCursorName = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicCursorName];
    public static final String dynamicCloseStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicCloseStatement];
    public static final String dynamicFetchStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicFetchStatement];
    public static final String dynamicDeleteStatementPositioned = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicDeleteStatementPositioned];
    public static final String dynamicUpdateStatementPositioned = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicUpdateStatementPositioned];
    
    public static final String sqlDiagnosticsStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlDiagnosticsStatement];
    public static final String getDiagnosticsStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_getDiagnosticsStatement];
    public static final String sqlDiagnosticsInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlDiagnosticsInformation];
    public static final String statementInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_statementInformation];
    public static final String statementInformationItem = Sql92Parser.ruleNames[Sql92Parser.RULE_statementInformationItem];
    public static final String statementInformationItemName = Sql92Parser.ruleNames[Sql92Parser.RULE_statementInformationItemName];
    public static final String conditionInformation = Sql92Parser.ruleNames[Sql92Parser.RULE_conditionInformation];
    public static final String conditionNumber = Sql92Parser.ruleNames[Sql92Parser.RULE_conditionNumber];
    public static final String conditionInformationItem = Sql92Parser.ruleNames[Sql92Parser.RULE_conditionInformationItem];
    public static final String conditionInformationItemName = Sql92Parser.ruleNames[Sql92Parser.RULE_conditionInformationItemName];
    
    public static final String statementOrDeclaration = Sql92Parser.ruleNames[Sql92Parser.RULE_statementOrDeclaration];
    
    public static final String preparableStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableStatement];
    public static final String preparableSqlDataStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableSqlDataStatement];
    public static final String dynamicSingleRowSelectStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicSingleRowSelectStatement];
    public static final String dynamicSelectStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_dynamicSelectStatement];
    public static final String preparableDynamicDeleteStatementPositioned = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableDynamicDeleteStatementPositioned];
    public static final String preparableDynamicUpdateStatementPositioned = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableDynamicUpdateStatementPositioned];
    public static final String preparableSqlSchemaStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableSqlSchemaStatement];
    public static final String preparableSqlTransactionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableSqlTransactionStatement];
    public static final String preparableSqlSessionStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_preparableSqlSessionStatement];
    
    public static final String directSqlStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_directSqlStatement];
    public static final String directSqlDataStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_directSqlDataStatement];
    public static final String selectStatement = Sql92Parser.ruleNames[Sql92Parser.RULE_selectStatement];
    
    public static final String sqlQueries = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlQueries];
    public static final String sqlQuery = Sql92Parser.ruleNames[Sql92Parser.RULE_sqlQuery];
}
