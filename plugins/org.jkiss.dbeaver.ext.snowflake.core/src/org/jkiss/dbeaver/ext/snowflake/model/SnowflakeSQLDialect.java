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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDollarQuoteRule;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLMultiWordRule;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

public class SnowflakeSQLDialect extends GenericSQLDialect implements TPRuleProvider {

    private static final String[][] SNOWFLAKE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END}
    };

    public SnowflakeSQLDialect() {
        super("Snowflake", "snowflake");
    }

    // Referenced from https://docs.snowflake.com/en/sql-reference/functions-all
    public static final String[] SNOWFLAKE_FUNCTIONS = new String[] {
        "ABS",                  "ACOS",
        "ACOSH",                "ADD_MONTHS",
        "ANY_VALUE",            "APPROX_COUNT_DISTINCT",
        "APPROX_PERCENTILE",    "APPROX_TOP_K",
        "ARRAY_AGG",            "ARRAY_CONTAINS",
        "ARRAY_DISTINCT",       "ARRAY_EXCEPT",
        "ARRAY_MAX",            "ARRAY_MIN",
        "ARRAY_POSITION",       "ARRAY_REMOVE",
        "ARRAY_SIZE",           "ARRAY_SORT",
        "ARRAYS_OVERLAP",       "ASCII",
        "ASIN",                 "ASINH",
        "ATAN",                 "ATAN2",
        "ATANH",                "AVG",
        "BASE64_ENCODE",        "BIT_LENGTH",
        "BITAND",               "BITNOT",
        "BITOR",                "BITXOR",
        "CBRT",                 "CEIL",
        "CHARINDEX",            "COALESCE",
        "COLLATION",            "COMPRESS",
        "CONCAT_WS",            "CONTAINS",
        "CORR",                 "COS",
        "COSH",                 "COT",
        "COUNT",                "COUNT_IF",
        "COVAR_POP",            "COVAR_SAMP",
        "CUME_DIST",            "CURRENT_DATABASE",
        "CURRENT_DATE",         "CURRENT_SCHEMA",
        "CURRENT_TIME",         "CURRENT_TIMESTAMP",
        "CURRENT_USER",         "CURRENT_VERSION",
        "DATE_PART",            "DATE_TRUNC",
        "DATEADD",              "DATEDIFF",
        "DAYNAME",              "DECODE",
        "DEGREES",              "DENSE_RANK",
        "ENCRYPT",              "ENDSWITH",
        "EQUAL_NULL",           "EXP",
        "EXTRACT",              "FACTORIAL",
        "FIRST_VALUE",          "FLATTEN",
        "FLOOR",                "GET",
        "GETBIT",               "GREATEST",
        "GROUPING",             "GROUPING_ID",
        "HASH",                 "HEX_ENCODE",
        "IFF",                  "IFNULL",
        "INITCAP",              "INSERT",
        "IS_BOOLEAN",           "KURTOSIS",
        "LAG",                  "LAST_DAY",
        "LAST_VALUE",           "LEAD",
        "LEAST",                "LEFT",
        "LISTAGG",              "LN",
        "LOG",                  "LOWER",
        "LPAD",                 "LTRIM",
        "MAP_CONTAINS_KEY",     "MAP_KEYS",
        "MAX",                  "MAX_BY",
        "MEDIAN",               "MIN",
        "MIN_BY",               "MOD",
        "MODE",                 "MONTHNAME",
        "MONTHS_BETWEEN",       "NEXT_DAY",
        "NTH_VALUE",            "NTILE",
        "NULLIF",               "NULLIFZERO",
        "NVL",                  "NVL2",
        "OCTET_LENGTH",         "PARSE_URL",
        "PERCENT_RANK",         "PERCENTILE_CONT",
        "PERCENTILE_DISC",      "PI",
        "POSITION",             "RADIANS",
        "RANDOM",               "RANK",
        "RATIO_TO_REPORT",      "REGEXP",
        "REGEXP_COUNT",         "REGEXP_INSTR",
        "REGEXP_LIKE",          "REGEXP_REPLACE",
        "REGEXP_SUBSTR",        "REGR_AVGX",
        "REGR_AVGY",            "REGR_COUNT",
        "REGR_INTERCEPT",       "REGR_R2",
        "REGR_SLOPE",           "REGR_SXX",
        "REGR_SXY",             "REGR_SYY",
        "REPEAT",               "REPLACE",
        "REVERSE",              "RIGHT",
        "RLIKE",                "ROUND",
        "ROW_NUMBER",           "RPAD",
        "RTRIM",                "SIGN",
        "SIN",                  "SINH",
        "SOUNDEX",              "SPACE",
        "SPLIT",                "SPLIT_PART",
        "SQRT",                 "ST_BUFFER",
        "ST_CENTROID",          "ST_CONTAINS",
        "ST_DIFFERENCE",        "ST_DIMENSION",
        "ST_DISJOINT",          "ST_DISTANCE",
        "ST_ENDPOINT",          "ST_ENVELOPE",
        "ST_LENGTH",            "ST_POINTN",
        "ST_SETSRID",           "ST_STARTPOINT",
        "ST_SYMDIFFERENCE",     "ST_TRANSFORM",
        "ST_UNION",             "ST_WITHIN",
        "ST_X",                 "ST_Y",
        "STARTSWITH",           "STDDEV",
        "STDDEV_POP",           "STDDEV_SAMP",
        "SUM",                  "SYSDATE",
        "TAN",                  "TANH",
        "TIMEDIFF",             "TIMESTAMPADD",
        "TIMESTAMPDIFF",        "TO_BINARY",
        "TO_BOOLEAN",           "TO_DOUBLE",
        "TO_JSON",              "TRANSLATE",
        "TRIM",                 "TRUNC",
        "TRY_CAST",             "TRY_TO_BINARY",
        "TYPEOF",               "UNICODE",
        "UPPER",                "VAR_POP",
        "VAR_SAMP",             "WIDTH_BUCKET",
        "ZEROIFNULL"
    };

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(
            Arrays.asList(
                "QUALIFY",
                "ILIKE",
                "PACKAGE",
                "PIPE",
                "STAGE",
                "STREAM",
                "TAG",
                "TASK"
            ));
        addFunctions(Arrays.asList(SNOWFLAKE_FUNCTIONS));
    }

    @NotNull
    @Override
    public TPRule[] extendRules(@Nullable DBPDataSourceContainer dataSource, @NotNull RulePosition position) {
        if (position == RulePosition.INITIAL || position == RulePosition.PARTITION) {
            boolean useDollarQuoteRule = dataSource == null ||
                CommonUtils.getBoolean(
                    dataSource.getConnectionConfiguration().getProviderProperty(SnowflakeConstants.PROP_DD_STRING),
                    dataSource.getPreferenceStore().getBoolean(SnowflakeConstants.PROP_DD_STRING) // backward compatibility
                );
            return new TPRule[] {
                new SQLDollarQuoteRule(
                    position == RulePosition.PARTITION,
                    false,
                    false,
                    useDollarQuoteRule
                )
            };
        }
        if (position == RulePosition.KEYWORDS) {
            final TPTokenDefault keywordToken = new TPTokenDefault(SQLTokenType.T_KEYWORD);
            return new TPRule[]{
                new SQLMultiWordRule(new String[]{"BEGIN", "TRANSACTION"}, keywordToken),
                new SQLMultiWordRule(new String[]{"IF", "EXISTS"}, keywordToken),
                new SQLMultiWordRule(new String[]{"IF", "NOT", "EXISTS"}, keywordToken)
            };
        }
        return new TPRule[0];
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return SNOWFLAKE_BEGIN_END_BLOCK;
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        // Without escaping of wildcards Snowflake reads all metadata directly from database and ignores specified objects names
        // #9875
        return "\\";
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String getColumnTypeModifiers(
        @NotNull DBPDataSource dataSource,
        @NotNull DBSTypedObject column,
        @NotNull String typeName,
        @NotNull DBPDataKind dataKind
    ) {
        Integer scale;
        switch (typeName) {
            case SnowflakeConstants.TYPE_NUMBER:
            case SnowflakeConstants.TYPE_NUMERIC:
            case SnowflakeConstants.TYPE_DECIMAL:
                DBSDataType dataType = DBUtils.getDataType(column);
                scale = column.getScale();
                int precision = CommonUtils.toInt(column.getPrecision());
                if (precision == 0 && dataType != null && scale != null && scale == dataType.getMinScale()) {
                    return "";
                }
                if (precision == 0 || precision > SnowflakeConstants.NUMERIC_MAX_PRECISION) {
                    precision = SnowflakeConstants.NUMERIC_MAX_PRECISION;
                }
                if (scale != null || precision > 0) {
                    // 38 - is default precision value. And we can not add scale here.
                    // It will be changed to 0 automatically after table creation from the database side.
                    return "(" + (precision > 0 ? precision : SnowflakeConstants.NUMERIC_MAX_PRECISION) +
                        (scale != null && scale > 0 ? "," + scale : "") +  ")";
                }
                break;
            case SQLConstants.DATA_TYPE_DOUBLE:
            case SnowflakeConstants.TYPE_DOUBLE_PRECISION:
            case SnowflakeConstants.TYPE_REAL:
            case SQLConstants.DATA_TYPE_FLOAT:
            case SQLConstants.DATA_TYPE_INT:
            case SnowflakeConstants.TYPE_INTEGER:
            case SQLConstants.DATA_TYPE_BIGINT:
                // These types do not have parameters
                return null;
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }
    
    @Override
    public boolean validIdentifierStart(char c) {
        return SQLUtils.isLatinLetter(c) || c == '_';
    }
    
    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return SQLUtils.isLatinLetter(c) || Character.isDigit(c) || c == '_' || (quoted && validCharacters.indexOf(c) != -1) || c == '$';
    }

    
    @Override
    public boolean mustBeQuoted(@NotNull String str, boolean forceCaseSensitive) {
        // Unquoted object identifiers:
        // * Start with a letter (A-Z, a-z) or an underscore (“_”).
        // * Contain only letters, underscores, decimal digits (0-9), and dollar signs (“$”).
        // * Are stored and resolved as uppercase characters (e.g. id is stored and resolved as ID).
        // https://docs.snowflake.com/en/sql-reference/identifiers-syntax

        if (str.isBlank()) {
            return true;
        }
        return super.mustBeQuoted(str, forceCaseSensitive);
    }
}
