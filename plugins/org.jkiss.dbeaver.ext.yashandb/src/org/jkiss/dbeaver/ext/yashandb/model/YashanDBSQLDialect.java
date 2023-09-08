/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.data.YashanDBBinaryFormatter;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDataTypeConverter;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLExpressionFormatter;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
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
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * Oracle SQL dialect
 */
public class YashanDBSQLDialect extends JDBCSQLDialect implements SQLDataTypeConverter {

    private static final Log log = Log.getLog(YashanDBSQLDialect.class);

    private static final String[] EXEC_KEYWORDS = new String[]{"call"};

    private static final String[] YASHANDB_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
            BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
            new String[]{
                    "CREATE", "ALTER", "DROP",
                    "ANALYZE", "VALIDATE",
            }
    );

    private static final String[][] YASHANDB_BEGIN_END_BLOCK = new String[][]{
            {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
            {"IF", SQLConstants.BLOCK_END+" IF"},
            {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
            {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " " + SQLConstants.KEYWORD_CASE}
    };

    private static final String[] YASHANDB_BLOCK_HEADERS = new String[] { "DECLARE", "FUNCTION", "PROCEDURE" };

    private static final String[] YASHANDB_INNER_BLOCK_PREFIXES = new String[]{
            "AS",
            "IS"
    };

    public static final String[] OTHER_TYPES_FUNCTIONS = {
            //functions without parentheses #8710
            "CURRENT_DATE",
            "CURRENT_TIMESTAMP",
            "DBTIMEZONE",
            "SESSIONTIMEZONE",
            "SYSDATE",
            "SYSTIMESTAMP"
    };
    public static final String[] YASHANDB_ALL_KEYWORD = {
            "ABS", "ABSOLUTE", "ACCESS", "ACOS", "ACTION", "ADD", "ADD_MONTHS", "AFTER", "ALL", "ALLOCATE",
            "ALL_PL_SQL_RESERVED_ WORDS", "ALTER", "ALWAYS", "ANALYZE", "AND", "ANY", "APPENDCHILDXML",
            "APPROX_COUNT_DISTINCT", "ARE", "ARRAY", "AS", "ASC", "ASCII", "ASCIISTR", "ASENSITIVE", "ASIN",
            "ASSERTION", "ASSIGNMENT", "ASYMMETRIC", "AT", "ATAN", "ATAN2", "ATOMIC", "ATTRIBUTE", "ATTRIBUTES",
            "AUDIT", "AUTHORIZATION", "AVG", "BEFORE", "BEGIN", "BETWEEN", "BFILENAME", "BIGINT", "BINARY",
            "BIN_TO_NUM", "BITAND", "BLOB", "BODY", "BOOLEAN", "BOTH", "BULK", "BY", "CALL", "CALLED", "CARDINALITY",
            "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CEIL", "CEILING", "CHAIN", "CHANGE", "CHAR", "CHARACTER",
            "CHARACTERISTICS", "CHARACTERS", "CHARACTER_LENGTH", "CHARTOROWID", "CHAR_LENGTH", "CHECK", "CHR", "CLOB",
            "CLOSE", "CLUSTER", "CLUSTER_ID", "CLUSTER_PROBABILITY", "CLUSTER_SET", "COALESCE", "COLLATE", "COLLATION",
            "COLLECT", "COLUMN", "COMMENT", "COMMIT", "COMMITTED", "COMPOSE", "COMPRESS", "COMPUTE", "CONCAT", "CONDITION",
            "CONNECT", "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONSTRUCTOR", "CONTAINS", "CONTINUE", "CONVERT", "CORR",
            "CORRESPONDING", "CORR_K", "CORR_S", "COS", "COSH", "COUNT", "COVAR_POP", "COVAR_SAMP", "CREATE", "CROSS", "CUBE",
            "CUME_DIST", "CURDATE", "CURRENT", "CURRENT_DATE", "CURRENT_TIMESTAMP", "CURSOR", "CURSOR_NAME", "CV", "CYCLE", "DATA",
            "DATABASE", "DATE", "DAY", "DBTIMEZONE", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DECODE", "DECOMPOSE", "DEFAULT", "DEFAULTS",
            "DEFERRABLE", "DEFERRED", "DEFINED", "DEFINER", "DEGREE", "DELETE", "DELETEXML", "DENSE_RANK", "DEPTH", "DEREF", "DERIVED",
            "DESC", "DESCRIBE", "DESCRIPTOR", "DETERMINISTIC", "DIAGNOSTICS", "DISCONNECT", "DISPATCH", "DISTINCT", "DOMAIN", "DOUBLE",
            "DROP", "DUMP", "DYNAMIC", "EACH", "ELEMENT", "ELSE", "ELSIF", "EMPTY_BLOB", "EMPTY_CLOB", "END", "END-EXEC", "EQUALS",
            "ESCAPE", "EVERY", "EXCEPT", "EXCEPTION", "EXCLUDE", "EXCLUDING", "EXCLUSIVE", "EXEC", "EXECUTE", "EXISTS", "EXISTSNODE",
            "EXIT", "EXP", "EXTERNAL", "EXTRACT", "EXTRACTVALUE", "FALSE", "FEATURE_ID", "FEATURE_SET", "FEATURE_VALUE", "FETCH",
            "FILE", "FILTER", "FINAL", "FIRST", "FIRST_VALUE", "FLOAT", "FLOOR", "FOLLOWING", "FOR", "FOREIGN", "FOUND",
            "FREE", "FROM", "FROM_TZ", "FULL", "FUNCTION", "FUSION", "GENERAL", "GENERATED", "GET", "GLOBAL", "GO", "GOTO",
            "GRANT", "GRANTED", "GREATEST", "GROUP", "GROUPING", "GROUPING_ID", "GROUP_ID", "HAVING", "HEXTORAW",
            "HIERARCHY", "HOLD", "HOUR", "IDENTIFIED", "IDENTITY", "IF", "IMMEDIATE", "IMPLEMENTATION", "IN",
            "INCLUDING", "INCREMENT", "INDEX", "INDICATOR", "INITCAP", "INITIAL", "INITIALLY", "INNER", "INOUT",
            "INPUT", "INSENSITIVE", "INSERT", "INSERTCHILDXML", "INSERTXMLBEFORE", "INSTANCE", "INSTANTIABLE", "INSTR", "INSTR2", "INSTR4", "INSTRB", "INSTRC", "INT", "INTEGER", "INTERSECT", "INTERSECTION", "INTERVAL", "INTO", "INVOKER", "IS", "ISOLATION", "ITERATION_NUMBER", "JOIN", "KEY", "KEY_TYPE", "LAG", "LANGUAGE", "LARGE", "LAST", "LAST_DAY", "LAST_VALUE", "LATERAL", "LCASE", "LEAD", "LEADING", "LEAST", "LEFT", "LENGTH", "LENGTHB", "LEVEL", "LIKE", "LIMIT", "LISTAGG", "LN", "LNNVL", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOCATOR", "LOCK", "LOG", "LOG10", "LONG", "LOOP", "LOWER", "LPAD", "LTRIM", "MAKE_REF", "MAP", "MATCH", "MATCHED", "MATERIALIZED", "MAX", "MAXEXTENTS", "MAXVALUE", "MEDIAN", "MEMBER", "MERGE", "METHOD", "MIN", "MINUS", "MINUTE", "MINVALUE", "MOD", "MODE", "MODIFIES", "MODIFY", "MONTH", "MONTHS_BETWEEN", "MORE", "MULTISET", "MUMPS", "NANVL", "NATIONAL", "NATURAL", "NESTING", "NEW", "NEW_TIME", "NEXT", "NEXT_DAY", "NLSSORT", "NLS_CHARSET_DECL_LEN", "NLS_CHARSET_ID", "NLS_CHARSET_NAME", "NLS_INITCAP", "NLS_LOWER", "NLS_UPPER", "NO", "NOAUDIT", "NOCOMPRESS", "NONE", "NORMALIZE", "NORMALIZED", "NOT", "NOWAIT", "NTILE", "NULL", "NULLIF", "NULLS", "NUMBER", "NUMERIC", "NUMTODSINTERVAL", "NUMTOYMINTERVAL", "NVL", "NVL2", "OBJECT", "OCTETS", "OCTET_LENGTH", "OF", "OFFLINE", "OLD", "ON", "ONLINE", "ONLY", "OPEN", "OPTION", "OPTIONS", "OR", "ORA_HASH", "ORDER", "ORDERING", "ORDINALITY", "OTHERS", "OUT", "OUTER", "OUTPUT", "OVER", "OVERLAPS", "OVERLAY", "OVERRIDING", "PACKAGE", "PAD", "PARAMETER", "PARTIAL", "PARTITION", "PATH", "PCTFREE", "PERCENTILE_CONT", "PERCENTILE_DISC", "PERCENT_RANK", "PI", "PLACING", "POSITION", "POWER", "POWERMULTISET", "POWERMULTISET_BY_CARDINALITY", "PRECEDING", "PRECISION", "PREDICTION", "PREDICTION_COST", "PREDICTION_DETAILS", "PREDICTION_PROBABILITY", "PREDICTION_SET", "PREPARE", "PRESENTNNV", "PRESENTV", "PRESERVE", "PREVIOUS", "PRIMARY", "PRIOR", "PRIVILEGES", "PROCEDURE", "RANGE", "RANK", "RATIO_TO_REPORT", "RAWTOHEX", "RAWTONHEX", "READ", "READS", "REAL", "RECORD", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "REFTOHEX", "REGEXP_COUNT", "REGEXP_INSTR", "REGEXP_LIKE", "REGEXP_REPLACE", "REGEXP_SUBSTR", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2", "REGR_SLOPE", "REGR_SXX", "REGR_SXY", "REGR_SYY", "RELATIVE", "RELEASE", "REMAINDER", "RENAME", "REPEATABLE", "REPLACE", "RESTART", "RESTRICT", "RESULT", "RETURN", "RETURNS", "REVERSE", "REVOKE", "RIGHT", "ROLE", "ROLLBACK", "ROLLUP", "ROUND", "ROUTINE", "ROW", "ROWIDTOCHAR", "ROWIDTONCHAR", "ROWS", "ROW_NUMBER", "RPAD", "RTRIM", "SAVEPOINT", "SCALE", "SCHEMA", "SCN_TO_TIMESTAMP", "SCOPE", "SCROLL", "SEARCH", "SECOND", "SECTION", "SECURITY", "SELECT", "SELF", "SENSITIVE", "SEQUENCE", "SERIALIZABLE", "SESSION", "SESSIONTIMEZONE", "SESSION_USER", "SET", "SETS", "SIGN", "SIMILAR", "SIN", "SINH", "SIZE", "SMALLINT", "SOME", "SOUNDEX", "SOURCE", "SPACE", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQRT", "START", "STATEMENT", "STATIC", "STATISTICS", "STATS_BINOMIAL_TEST", "STATS_CROSSTAB", "STATS_F_TEST", "STATS_KS_TEST", "STATS_MODE", "STATS_MW_TEST", "STATS_ONE_WAY_ANOVA", "STATS_T_TEST_INDEP", "STATS_T_TEST_INDEPU", "STATS_T_TEST_ONE", "STATS_T_TEST_PAIRED", "STATS_WSR_TEST", "STDDEV", "STRUCTURE", "STYLE", "SUBMULTISET", "SUBSTR", "SUBSTR2", "SUBSTR4", "SUBSTRB", "SUBSTRC", "SUBSTRING", "SUM", "SYMMETRIC", "SYSDATE", "SYSTEM_USER", "SYSTIMESTAMP", "SYS_CONNECT_BY_PATH", "SYS_CONTEXT", "SYS_DBURIGEN", "SYS_EXTRACT_UTC", "SYS_GUID", "SYS_TYPEID", "SYS_XMLAGG", "SYS_XMLGEN", "TABLE", "TABLESAMPLE", "TAN", "TANH", "TEMPORARY", "THEN", "TIES", "TIME", "TIMESTAMP", "TIMESTAMP_TO_SCN", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TO_BINARY_DOUBLE", "TO_BINARY_FLOAT", "TO_CHAR", "TO_CLOB", "TO_DATE", "TO_DSINTERVAL", "TO_LOB", "TO_MULTI_BYTE", "TO_NCHAR", "TO_NCLOB", "TO_NUMBER", "TO_SINGLE_BYTE", "TO_TIMESTAMP", "TO_TIMESTAMP_TZ", "TO_YMINTERVAL", "TRAILING", "TRANSACTION", "TRANSFORM", "TRANSFORMS", "TRANSLATE", "TRANSLATION", "TREAT", "TRIGGER", "TRIM", "TRUE", "TRUNC", "TRUNCATE", "TYPE", "TZ_OFFSET", "UCASE", "UESCAPE", "UID", "UNBOUNDED", "UNCOMMITTED", "UNDER", "UNION", "UNIQUE", "UNISTR", "UNKNOWN", "UNNAMED", "UNNEST", "UPDATE", "UPDATEXML", "UPPER", "USAGE", "USER", "USERENV", "USING", "VALIDATE", "VALUES", "VARBINARY", "VARCHAR", "VARIANCE", "VARYING", "VIEW", "VSIZE", "WHEN", "WHENEVER", "WHERE", "WHILE", "WIDTH_BUCKET", "WINDOW", "WITH", "WITHIN", "WITHOUT", "WORK", "WRAPPED", "WRITE", "XMLAGG", "XMLCDATA", "XMLCOLATTVAL", "XMLCOMMENT", "XMLCONCAT", "XMLFOREST", "XMLPARSE", "XMLPI", "XMLQUERY", "XMLROOT", "XMLSEQUENCE", "XMLSERIALIZE", "XMLTABLE", "XMLTRANSFORM", "YEAR", "ZONE", "call"

    };

    public static final String[] ADVANCED_KEYWORDS = {
            "SYNONYM", "CREATE OR REPLACE", "NEXTVAL",
            "REPLACEex",
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
    };
    private boolean crlfBroken;
    private DBPPreferenceStore preferenceStore;

    private SQLTokenPredicateSet cachedDialectSkipTokenPredicates = null;

    public YashanDBSQLDialect() {
        super("YashanDB", "yashandb");
        log.debug(">>>Initialize {YashanDBSQLDialect}....");
        setUnquotedIdentCase(DBPIdentifierCase.UPPER);
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        crlfBroken = !dataSource.isServerVersionAtLeast(11, 0);
        preferenceStore = dataSource.getContainer().getPreferenceStore();

        //java.sql.DatabaseMetaData.getIdentifierQuoteString, if jdbc does not setting.
        setIdentifierQuoteString(DEFAULT_IDENTIFIER_QUOTES);

        addFunctions(
                Arrays.asList(
                        "DBMS_OUTPUT.PUT_LINE",
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
        //removeSQLKeyword("SYSTEM");

        for (String kw : ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }

        addKeywords(Arrays.asList(OTHER_TYPES_FUNCTIONS), DBPKeywordType.OTHER);
        turnFunctionIntoKeyword("TRUNCATE");

        cachedDialectSkipTokenPredicates = makeDialectSkipTokenPredicates(dataSource);
    }

    /**
     * Put all prompts in getDMLKeywords
     * @return String[]
     */
    @Override
    public String[] getDMLKeywords() {
        return YASHANDB_ALL_KEYWORD;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return YASHANDB_BEGIN_END_BLOCK;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return YASHANDB_BLOCK_HEADERS;
    }

    @Nullable
    @Override
    public String[] getInnerBlockPrefixes() {
        return YASHANDB_INNER_BLOCK_PREFIXES;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String getLikeEscapeClause(@NotNull String escapeChar) {
        return " ESCAPE " + getQuotedString(escapeChar);
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
            // These special values should be quoted, as shown in the example below.
            return '\'' + String.valueOf(value) + '\'';
        }

        //    com.yashandb.core.DataType   public static final int DS_INTERVAL = 20;
        String fullTypeName = attribute.getFullTypeName();
        if (fullTypeName.contains("INTERVAL")|| fullTypeName.contains("TIME"))  //时间字段要加 '
            return '\'' + String.valueOf(value) + '\'';

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
        return YashanDBBinaryFormatter.INSTANCE;
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return "DUAL";
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return YASHANDB_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        String schemaName = proc.getParentObject().getName();
        return "CALL " + schemaName + "." + proc.getName();
    }

    //TODO:待优化
    @Override
    public boolean isDisableScriptEscapeProcessing() {
        return preferenceStore == null || preferenceStore.getBoolean(YashanDBConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING);
    }

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return super.getScriptDelimiters();
    }

    //@Override
    //public boolean isCRLFBroken() {
    //    return crlfBroken;
    //}

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName,
                                         @NotNull DBPDataKind dataKind) {
        Integer scale;
        switch (typeName) {
            case YashanDBConstants.TYPE_NUMBER:
            case YashanDBConstants.TYPE_DECIMAL:
                DBSDataType dataType = DBUtils.getDataType(column);
                scale = column.getScale();
                int precision = CommonUtils.toInt(column.getPrecision());
                if (precision == 0 && dataType != null && scale != null && scale == dataType.getMinScale()) {
                    return "";
                }
                if (precision == 0 || precision > YashanDBConstants.NUMERIC_MAX_PRECISION) {
                    precision = YashanDBConstants.NUMERIC_MAX_PRECISION;
                }
                if (scale != null || precision > 0) {
                    // 38 - is default precision value. And we can not add scale here.
                    // It will be changed to 0 automatically after table creation from the Oracle side.
                    return "(" + (precision > 0 ? precision : "38") + (scale != null ? "," + scale : "") + ")";
                }
                    break;
            case YashanDBConstants.TYPE_INTERVAL_DAY_SECOND:
                // This interval type has fractional seconds precision. In bounds from 0 to 9. We can show this parameter.
                // FIXME: This type has day precision inside type name. Like INTERVAL DAY(2) TO SECOND(6). So far we can't show it (But
                //  we do it in Column Manager)
                scale = column.getScale();
                if (scale == null) {
                    return "";
                }
                if (scale < 0 || scale > 9) {
                    scale = YashanDBConstants.INTERVAL_DEFAULT_SECONDS_PRECISION;
                }
                return "(" + scale + ")";
            case YashanDBConstants.TYPE_NAME_BFILE:
            case YashanDBConstants.TYPE_NAME_CFILE:
            case YashanDBConstants.TYPE_CONTENT_POINTER:
            case YashanDBConstants.TYPE_LONG:
            case YashanDBConstants.TYPE_LONG_RAW:
            case YashanDBConstants.TYPE_OCTET:
            case YashanDBConstants.TYPE_INTERVAL_YEAR_MONTH:
                // Don't add modifiers to these types
                return "";
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }

    @Override
    public String convertExternalDataType(@NotNull SQLDialect sourceDialect, @NotNull DBSTypedObject sourceTypedObject,
                                          @Nullable DBPDataTypeProvider targetTypeProvider) {
        String type = super.convertExternalDataType(sourceDialect, sourceTypedObject, targetTypeProvider);
        if (type != null) {
            return type;
        }
        String externalTypeName = sourceTypedObject.getTypeName().toUpperCase(Locale.ENGLISH);
        String localDataType = null, dataTypeModifies = null;

        switch (externalTypeName) {
            case "VARCHAR":
                //We don't want to use a VARCHAR it's not recommended
                localDataType = YashanDBConstants.TYPE_NAME_VARCHAR2;
                break;
            case "XML":
            case "XMLTYPE":
                localDataType = YashanDBConstants.TYPE_FQ_XML;
                break;
            case "JSON":
            case "JSONB":
                localDataType = "JSON";
                break;
            case "GEOMETRY":
            case "GEOGRAPHY":
            case "SDO_GEOMETRY":
                localDataType = YashanDBConstants.TYPE_FQ_GEOMETRY;
                break;
            case "NUMERIC":
                localDataType = YashanDBConstants.TYPE_NUMBER;
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

        // by following through Get Started links till the SQL Language Reference link presented
        TokenPredicateSet conditions = TokenPredicateSet.of(
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
                )
        );

        conditions.add(new TokenPredicatesCondition(
                SQLParserActionKind.SKIP_SUFFIX_TERM,
                tt.token("WITH"),
                tt.sequence("END", ";")
        ));

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
}
