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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.data.OracleBinaryFormatter;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLParserActionKind;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicatesCondition;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Oracle SQL dialect
 */
public class OracleSQLDialect extends JDBCSQLDialect
    implements SQLDataTypeConverter, SQLDialectDDLExtension, SQLDialectSchemaController {

    private static final Log log = Log.getLog(OracleSQLDialect.class);

    private static final String[] EXEC_KEYWORDS = new String[]{"call"};

    private static final String[] ORACLE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "CREATE", "ALTER", "DROP",
            "ANALYZE", "VALIDATE",
        }
    );

    private static final String[][] ORACLE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " " + SQLConstants.KEYWORD_CASE},
    };

    private static final String[] ORACLE_BLOCK_HEADERS = new String[]{
        "DECLARE",
        "PACKAGE"
    };

    private static final String[] ORACLE_INNER_BLOCK_PREFIXES = new String[]{
        "AS",
        "IS",
    };

    private static final String[] OTHER_TYPES_FUNCTIONS = {
        //functions without parentheses #8710
        "CURRENT_DATE",
        "CURRENT_TIMESTAMP",
        "DBTIMEZONE",
        "SESSIONTIMEZONE",
        "SYSDATE",
        "SYSTIMESTAMP"
    };

    private static final String[] ADVANCED_KEYWORDS = {
        "REPLACE",
        "PACKAGE",
        "FUNCTION",
        "TYPE",
        "BODY",
        "RECORD",
        "TRIGGER",
        "MATERIALIZED",
        "IF",
        "EACH",
        "RETURN",
        "WRAPPED",
        "AFTER",
        "BEFORE",
        "DATABASE",
        "ANALYZE",
        "VALIDATE",
        "STRUCTURE",
        "COMPUTE",
        "STATISTICS",
        "LOOP",
        "WHILE",
        "BULK",
        "ELSIF",
        "EXIT",
        "SUBPARTITION",
        "TEMPFILE",
        "DATAFILE",
        "TABLESPACE"
    };

    private static final String AUTO_INCREMENT_KEYWORD = "GENERATED ALWAYS AS IDENTITY";
    private boolean crlfBroken;
    private DBPPreferenceStore preferenceStore;

    private SQLTokenPredicateSet cachedDialectSkipTokenPredicates = null;

    public OracleSQLDialect() {
        super("Oracle", "oracle");
        setUnquotedIdentCase(DBPIdentifierCase.UPPER);
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        crlfBroken = !dataSource.isServerVersionAtLeast(11, 0);
        preferenceStore = dataSource.getContainer().getPreferenceStore();

        addFunctions(
            Arrays.asList(
                "SUBSTR", "APPROX_COUNT_DISTINCT",
                "REGEXP_SUBSTR", "REGEXP_INSTR", "REGEXP_REPLACE", "REGEXP_LIKE", "REGEXP_COUNT",
                // Additions from #323
                //Number Functions:
                "BITAND",
                "COSH",
                "NANVL",
                "REMAINDER",
                "SINH",
                "TANH",
                "TRUNC",

                //Character Functions Returning Character Values:
                "CHR",
                "INITCAP",
                "LPAD",
                "NLS_INITCAP",
                "NLS_LOWER",
                "NLSSORT",
                "NLS_UPPER",
                "RPAD",
                "REVERSE",
                "SUBSTRB",
                "SUBSTRC",
                "SUBSTR2",
                "SUBSTR4",

                // NLS Character Functions:
                "NLS_CHARSET_DECL_LEN",
                "NLS_CHARSET_ID",
                "NLS_CHARSET_NAME",

                //Character Functions Returning Number VALUES:
                "INSTR",
                "INSTRB",
                "INSTRC",
                "INSTR2",
                "INSTR4",
                "LENGTHB",
                "LENGTH",

                //Datetime Functions:
                "ADD_MONTHS",
                "FROM_TZ",
                "LAST_DAY",
                "MONTHS_BETWEEN",
                "NEW_TIME",
                "NEXT_DAY",
                "NUMTODSINTERVAL",
                "NUMTOYMINTERVAL",
                "SYS_EXTRACT_UTC",
                "TO_CHAR",
                "TO_TIMESTAMP",
                "TO_TIMESTAMP_TZ",
                "TO_DSINTERVAL",
                "TO_YMINTERVAL",
                "TRUNC",
                "TZ_OFFSET",

                //General Comparison Functions:
                "GREATEST",
                "LEAST",

                //Conversion Functions:
                "ASCIISTR",
                "BIN_TO_NUM",
                "CHARTOROWID",
                "COMPOSE",
                "DECOMPOSE",
                "HEXTORAW",
                "NUMTODSINTERVAL",
                "NUMTOYMINTERVAL",
                "RAWTOHEX",
                "RAWTONHEX",
                "ROWIDTOCHAR",
                "ROWIDTONCHAR",
                "SCN_TO_TIMESTAMP",
                "TIMESTAMP_TO_SCN",
                "TO_BINARY_DOUBLE",
                "TO_BINARY_FLOAT",
                "TO_CHAR",
                "TO_CLOB",
                "TO_DATE",
                "TO_DSINTERVAL",
                "TO_LOB",
                "TO_MULTI_BYTE",
                "TO_NCHAR",
                "TO_NCLOB",
                "TO_NUMBER",
                "TO_DSINTERVAL",
                "TO_SINGLE_BYTE",
                "TO_TIMESTAMP",
                "TO_TIMESTAMP_TZ",
                "TO_YMINTERVAL",
                "TO_YMINTERVAL",
                "UNISTR",

                //Large Object Functions:
                "BFILENAME",
                "EMPTY_BLOB",
                "EMPTY_CLOB",

                //Collection Functions:
                "POWERMULTISET",
                "POWERMULTISET_BY_CARDINALITY",

                //Hierarchical FUNCTION:
                "SYS_CONNECT_BY_PATH",

                //Data Mining Functions:
                "CLUSTER_ID",
                "CLUSTER_PROBABILITY",
                "CLUSTER_SET",
                "FEATURE_ID",
                "FEATURE_SET",
                "FEATURE_VALUE",
                "PREDICTION",
                "PREDICTION_COST",
                "PREDICTION_DETAILS",
                "PREDICTION_PROBABILITY",
                "PREDICTION_SET",

                //XML Functions:
                "APPENDCHILDXML",
                "DELETEXML",
                "DEPTH",
                "EXISTSNODE",
                "EXTRACTVALUE",
                "INSERTCHILDXML",
                "INSERTXMLBEFORE",
                "PATH",
                "SYS_DBURIGEN",
                "SYS_XMLAGG",
                "SYS_XMLGEN",
                "UPDATEXML",
                "XMLAGG",
                "XMLCDATA",
                "XMLCOLATTVAL",
                "XMLCOMMENT",
                "XMLCONCAT",
                "XMLFOREST",
                "XMLPARSE",
                "XMLPI",
                "XMLQUERY",
                "XMLROOT",
                "XMLSEQUENCE",
                "XMLSERIALIZE",
                "XMLTABLE",
                "XMLTRANSFORM",

                //Encoding and Decoding Functions:
                "DECODE",
                "DUMP",
                "ORA_HASH",
                "VSIZE",

                //NULL-Related Functions:
                "LNNVL",
                "NVL",
                "NVL2",

                //Environment and Identifier Functions:
                "SYS_CONTEXT",
                "SYS_GUID",
                "SYS_TYPEID",
                "UID",
                "USERENV",

                //Aggregate Functions:
                "CORR_S",
                "CORR_K",
                "FIRST",
                "GROUP_ID",
                "GROUPING_ID",
                "LAST",
                "MEDIAN",
                "STATS_BINOMIAL_TEST",
                "STATS_CROSSTAB",
                "STATS_F_TEST",
                "STATS_KS_TEST",
                "STATS_MODE",
                "STATS_MW_TEST",
                "STATS_ONE_WAY_ANOVA",
                "STATS_T_TEST_ONE",
                "STATS_T_TEST_PAIRED",
                "STATS_T_TEST_INDEP",
                "STATS_T_TEST_INDEPU",
                "STATS_WSR_TEST",
                "STDDEV",
                "VARIANCE",

                //Analytic Functions:
                "FIRST",
                "FIRST_VALUE",
                "LAG",
                "LAST",
                "LAST_VALUE",
                "LEAD",
                "NTILE",
                "RATIO_TO_REPORT",
                "STDDEV",
                "VARIANCE",
                "COALESCE",

                //Object Reference Functions:
                "MAKE_REF",
                "REFTOHEX",

                //Model Functions:
                "CV",
                "ITERATION_NUMBER",
                "PRESENTNNV",
                "PRESENTV",
                "PREVIOUS",

                // Other #4134
                "EXTRACT",
                "LISTAGG",
                "OVER",
                "RANK"
            ));
        removeSQLKeyword("SYSTEM");

        for (String kw : ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }

        addKeywords(Arrays.asList(OTHER_TYPES_FUNCTIONS), DBPKeywordType.OTHER);
        turnFunctionIntoKeyword("TRUNCATE");

        cachedDialectSkipTokenPredicates = makeDialectSkipTokenPredicates(dataSource);
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(OracleDataType.PREDEFINED_TYPES.keySet());
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return ORACLE_BEGIN_END_BLOCK;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return ORACLE_BLOCK_HEADERS;
    }

    @Nullable
    @Override
    public String[] getInnerBlockPrefixes() {
        return ORACLE_INNER_BLOCK_PREFIXES;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.INSERT_ALL;
    }

    @Override
    public String getLikeEscapeClause(@NotNull String escapeChar) {
        return " ESCAPE " + getQuotedString(escapeChar);
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
            // These special values should be quoted, as shown in the example below
            // https://docs.oracle.com/cd/B19306_01/server.102/b14200/functions090.htm
            return '\'' + String.valueOf(value) + '\'';
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return true;
    }

    @Override
    public boolean supportsAsKeywordBeforeAliasInFromClause() {
        return false;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @Nullable
    @Override
    public SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator) {
        if (operator == DBCLogicalOperator.LIKE) {
            return (left, right) -> "UPPER(" + left + ") LIKE UPPER(" + right + ")";
        }
        return super.getCaseInsensitiveExpressionFormatter(operator);
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return true;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return OracleBinaryFormatter.INSTANCE;
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return "DUAL";
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return ORACLE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        if (proc.getProcedureType() == DBSProcedureType.FUNCTION) {
            return SQLConstants.KEYWORD_SELECT + " " + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
        } else {
            return "CALL " + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
        }
    }

    @NotNull
    @Override
    protected String getProcedureCallEndClause(DBSProcedure procedure) {
        if (procedure.getProcedureType() == DBSProcedureType.FUNCTION) {
            return "FROM DUAL";
        }
        return super.getProcedureCallEndClause(procedure);
    }

    @Override
    public boolean isDisableScriptEscapeProcessing() {
        return preferenceStore == null || preferenceStore.getBoolean(OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING);
    }

    @Override
    public boolean supportsUuid() {
        return false;
    }

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return super.getScriptDelimiters();
    }

    @Override
    public boolean isCRLFBroken() {
        return crlfBroken;
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        Integer scale;
        switch (typeName) {
            case OracleConstants.TYPE_NUMBER:
            case OracleConstants.TYPE_DECIMAL:
                DBSDataType dataType = DBUtils.getDataType(column);
                scale = column.getScale();
                int precision = CommonUtils.toInt(column.getPrecision());
                if (precision == 0 && dataType != null && scale != null && scale == dataType.getMinScale()) {
                    return "";
                }
                if (precision == 0 || precision > OracleConstants.NUMERIC_MAX_PRECISION) {
                    precision = OracleConstants.NUMERIC_MAX_PRECISION;
                }
                if (scale != null || precision > 0) {
                    // 38 - is default precision value. And we can not add scale here.
                    // It will be changed to 0 automatically after table creation from the Oracle side.
                    return "(" + (precision > 0 ? precision : "38") + (scale != null ? "," + scale : "") +  ")";
                }
                break;
            case OracleConstants.TYPE_INTERVAL_DAY_SECOND:
                // This interval type has fractional seconds precision. In bounds from 0 to 9. We can show this parameter.
                // FIXME: This type has day precision inside type name. Like INTERVAL DAY(2) TO SECOND(6). So far we can't show it (But we do it in Column Manager)
                scale = column.getScale();
                if (scale == null) {
                    return "";
                }
                if (scale < 0 || scale > 9) {
                    scale = OracleConstants.INTERVAL_DEFAULT_SECONDS_PRECISION;
                }
                return "(" + scale + ")";
            case OracleConstants.TYPE_NAME_BFILE:
            case OracleConstants.TYPE_NAME_CFILE:
            case OracleConstants.TYPE_CONTENT_POINTER:
            case OracleConstants.TYPE_LONG:
            case OracleConstants.TYPE_LONG_RAW:
            case OracleConstants.TYPE_OCTET:
            case OracleConstants.TYPE_INTERVAL_YEAR_MONTH:
                // Don't add modifiers to these types
                return "";
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }

    @Override
    public String convertExternalDataType(@NotNull SQLDialect sourceDialect, @NotNull DBSTypedObject sourceTypedObject, @Nullable DBPDataTypeProvider targetTypeProvider) {
        String type = super.convertExternalDataType(sourceDialect, sourceTypedObject, targetTypeProvider);
        if (type != null) {
            return type;
        }
        String externalTypeName = sourceTypedObject.getTypeName().toUpperCase(Locale.ENGLISH);
        String localDataType = null, dataTypeModifies = null;

        switch (externalTypeName) {
            case "VARCHAR":
                //We don't want to use a VARCHAR it's not recommended
                //See https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-DF7E10FC-A461-4325-A295-3FD4D150809E
                localDataType = OracleConstants.TYPE_NAME_VARCHAR2;
                if (sourceTypedObject.getMaxLength() > 0
                    && sourceTypedObject.getMaxLength() != Integer.MAX_VALUE
                    && sourceTypedObject.getMaxLength() != Long.MAX_VALUE) {
                    dataTypeModifies = String.valueOf(sourceTypedObject.getMaxLength());
                }
                break;
            case "XML":
            case "XMLTYPE":
                localDataType = OracleConstants.TYPE_FQ_XML;
                break;
            case "JSON":
            case "JSONB":
                localDataType = "JSON";
                break;
            case "GEOMETRY":
            case "GEOGRAPHY":
            case "SDO_GEOMETRY":
                localDataType = OracleConstants.TYPE_FQ_GEOMETRY;
                break;
            case "NUMERIC":
                localDataType = OracleConstants.TYPE_NUMBER;
                if (sourceTypedObject.getPrecision() != null) {
                    dataTypeModifies = sourceTypedObject.getPrecision().toString();
                    if (sourceTypedObject.getScale() != null) {
                        dataTypeModifies += "," + sourceTypedObject.getScale();
                    }
                }
                break;
        }
        if (localDataType == null) {
            return null;
        }
        if (targetTypeProvider != null) {
            try {
                DBSDataType dataType = targetTypeProvider.resolveDataType(new VoidProgressMonitor(), localDataType);
                if (dataType == null) {
                    return null;

                }
                String targetTypeName = DBUtils.getObjectFullName(dataType, DBPEvaluationContext.DDL);
                if (dataTypeModifies != null) {
                    targetTypeName += "(" + dataTypeModifies + ")";
                }
                return targetTypeName;
            } catch (DBException e) {
                log.debug("Error resolving local data type", e);
                return null;
            }
        }
        return localDataType;
    }

    @Override
    @NotNull
    public SQLTokenPredicateSet getSkipTokenPredicates() {
        return cachedDialectSkipTokenPredicates == null ? super.getSkipTokenPredicates() : cachedDialectSkipTokenPredicates;
    }

    @NotNull
    private SQLTokenPredicateSet makeDialectSkipTokenPredicates(JDBCDataSource dataSource) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(this, dataSource.getContainer().getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        TokenPredicateFactory tt = TokenPredicateFactory.makeDialectSpecificFactory(ruleManager);

        // Oracle SQL references could be found from https://docs.oracle.com/en/database/oracle/oracle-database/
        // by following through Get Started links till the SQL Language Reference link presented

        TokenPredicateSet conditions = TokenPredicateSet.of(
                // https://docs.oracle.com/en/database/oracle/oracle-database/12.2/lnpls/CREATE-PACKAGE-BODY-statement.html#GUID-68526FF2-96A1-4F14-A10B-4DD3E1CD80BE
                // also presented in the earliest found reference on 7.3, so considered as always supported https://docs.oracle.com/pdf/A32538_1.pdf
                new TokenPredicatesCondition(
                        SQLParserActionKind.BEGIN_BLOCK,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")),
                                "PACKAGE", "BODY"
                        ),
                        tt.sequence()
                ),
                // https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/CREATE-FUNCTION.html#GUID-156AEDAC-ADD0-4E46-AA56-6D1F7CA63306
                // https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/CREATE-PROCEDURE.html#GUID-771879D8-BBFD-4D87-8A6C-290102142DA3
                // not fully described, only some cases partially discovered
                new TokenPredicatesCondition(
                        SQLParserActionKind.SKIP_SUFFIX_TERM,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")),
                                tt.alternative("FUNCTION", "PROCEDURE")
                        ),
                        tt.sequence(tt.alternative(
                                tt.sequence("RETURN", SQLTokenType.T_TYPE),
                                "deterministor", "pipelined", "parallel_enable", "result_cache",
                                ")",
                                tt.sequence("procedure", SQLTokenType.T_OTHER),
                                tt.sequence(SQLTokenType.T_OTHER, SQLTokenType.T_TYPE)
                        ), ";")
                ),
                new TokenPredicatesCondition(
                    SQLParserActionKind.BEGIN_BLOCK,
                    tt.sequence(),
                    tt.sequence(tt.not("END"), "IF", tt.not("EXISTS"))
                )
        );



        if (dataSource.isServerVersionAtLeast(12, 1)) {
            // for WITH procedures and functions prepending select clause introduced in 12.1
            //     https://oracle-base.com/articles/12c/with-clause-enhancements-12cr1
            // notation presented in https://docs.oracle.com/en/database/oracle/oracle-database/18/sqlrf/SELECT.html
            // but missing in https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_10002.htm
            conditions.add(new TokenPredicatesCondition(
                    SQLParserActionKind.SKIP_SUFFIX_TERM,
                    tt.token("WITH"),
                    tt.sequence("END", ";")
            ));
        }

        return conditions;
    }

    @Override
    public boolean hasCaseSensitiveFiltration() {
        return true;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }

    @Override
    public String getOffsetLimitQueryPart(int offset, int limit) {
        return String.format("OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
    }

    @Override
    public String getClobComparingPart(@NotNull String columnName) {
        return "DBMS_LOB.COMPARE(%s,?) = 0".formatted(columnName);
    }

    @Nullable
    @Override
    public String getAutoIncrementKeyword() {
        return AUTO_INCREMENT_KEYWORD;
    }

    @Override
    public boolean supportsCreateIfExists() {
        return false;
    }

    @NotNull
    @Override
    public String getTimestampDataType() {
        return OracleConstants.TYPE_NAME_TIMESTAMP;
    }

    @NotNull
    @Override
    public String getBigIntegerType() {
        return OracleConstants.TYPE_NUMBER;
    }

    @NotNull
    @Override
    public String getClobDataType() {
        return OracleConstants.TYPE_CLOB;
    }

    @NotNull
    @Override
    public String getBlobDataType() {
        return OracleConstants.TYPE_NAME_BLOB;
    }

    @NotNull
    @Override
    public String getUuidDataType() {
        return OracleConstants.TYPE_UUID;
    }

    @NotNull
    @Override
    public String getBooleanDataType() {
        return OracleConstants.TYPE_BOOLEAN;
    }

    @NotNull
    @Override
    public String getAlterColumnOperation() {
        return OracleConstants.OPERATION_MODIFY;
    }

    @Override
    public boolean supportsNoActionIndex() {
        return false;
    }

    @Override
    public boolean supportsAlterColumnSet() {
        return false;
    }

    @Override
    public boolean supportsAlterHasColumn() {
        return false;
    }

    @Override
    public boolean needsDefaultDataTypes() {
        return false;
    }

    @NotNull
    @Override
    public String getSchemaExistQuery(@NotNull String schemaName) {
        return "SELECT 1 FROM all_users WHERE USERNAME='" + schemaName + "'";
    }

    @NotNull
    @Override
    public String getCreateSchemaQuery(@NotNull String schemaName) {
        return "CREATE USER " + schemaName + " IDENTIFIED BY \"" + SecurityUtils.generatePassword(10) + "\"";
    }

    @Override
    public EnumSet<ProjectionAliasVisibilityScope> getProjectionAliasVisibilityScope() {
        return EnumSet.of(
            ProjectionAliasVisibilityScope.ORDER_BY
        );
    }
}
