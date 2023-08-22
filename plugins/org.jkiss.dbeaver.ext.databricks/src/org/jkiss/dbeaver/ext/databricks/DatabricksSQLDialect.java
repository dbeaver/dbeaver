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

package org.jkiss.dbeaver.ext.databricks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Arrays;

public class DatabricksSQLDialect extends GenericSQLDialect {

    private static final String[][] DEFAULT_QUOTE_STRINGS = {{"`", "`"}};

    public DatabricksSQLDialect() {
        super("SparkSQL", "spark");
    }

    private static final String[] SPARK_EXTRA_KEYWORDS = {"SHOW"};

    //see https://docs.databricks.com/sql/language-manual/sql-ref-functions-builtin-alpha.html
    private static final String[] SPARK_FUNCTIONS =
        {"ABS", "ACOS", "ACOSH", "ADD_MONTHS", "AES_DECRYPT", "AES_ENCRYPT", "AGGREGATE", "AND", "ANY", "ANY_VALUE",
            "APPROX_COUNT_DISTINCT", "APPROX_PERCENTILE", "APPROX_TOP_K", "ARRAY", "ARRAY_AGG", "ARRAY_CONTAINS", "ARRAY_DISTINCT",
            "ARRAY_EXCEPT", "ARRAY_INTERSECT", "ARRAY_JOIN", "ARRAY_MAX", "ARRAY_MIN", "ARRAY_POSITION", "ARRAY_REMOVE", "ARRAY_REPEAT",
            "ARRAY_SIZE", "ARRAY_SORT", "ARRAY_UNION", "ARRAYS_OVERLAP", "ARRAYS_ZIP", "ASCII", "ASIN", "ASINH", "ASSERT_TRUE", "ATAN",
            "ATAN2", "ATANH", "AVG", "BASE64", "BETWEEN", "BIGINT", "BIN", "BINARY", "BIT_AND", "BIT_COUNT", "BIT_GET", "BIT_LENGTH",
            "BIT_OR", "BIT_REVERSE", "BIT_XOR", "BOOL_AND", "BOOL_OR", "BOOLEAN", "BROUND", "BTRIM", "CARDINALITY", "CAST", "CBRT", "CEIL",
            "CEILING", "CHAR", "CHAR_LENGTH", "CHARACTER_LENGTH", "CHARINDEX", "CHR", "CLOUD_FILES_STATE", "COALESCE", "COLLECT_LIST",
            "COLLECT_SET", "CONCAT", "CONCAT_WS", "CONTAINS", "CONV", "CORR", "COS", "COSH", "COT", "COUNT", "COUNT_IF", "COUNT_MIN_SKETCH",
            "COVAR_POP", "COVAR_SAMP", "CRC32", "CSC", "CUBE", "CUME_DIST", "CURDATE", "CURRENT_CATALOG", "CURRENT_DATABASE",
            "CURRENT_DATE", "CURRENT_METASTORE", "CURRENT_SCHEMA", "CURRENT_TIMESTAMP", "CURRENT_TIMEZONE", "CURRENT_USER",
            "CURRENT_VERSION", "DATE", "DATE_ADD", "DATE_FORMAT", "DATE_FROM_UNIX_DATE", "DATE_PART", "DATE_SUB", "DATE_TRUNC", "DATEADD",
            "DATEADD", "DATEDIFF", "DATEDIFF", "DAY", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR", "DECIMAL", "DECODE", "DECODE", "DEGREES",
            "DENSE_RANK", "DOUBLE", "E", "ELEMENT_AT", "ELT", "ENCODE", "ENDSWITH", "EQUAL_NULL", "EVERY", "EXISTS", "EXP", "EXPLODE",
            "EXPLODE_OUTER", "EXPM1", "EXTRACT", "FACTORIAL", "FILTER", "FIND_IN_SET", "FIRST", "FIRST_VALUE", "FLATTEN", "FLOAT", "FLOOR",
            "FORALL", "FORMAT_NUMBER", "FORMAT_STRING", "FROM_CSV", "FROM_JSON", "FROM_UNIXTIME", "FROM_UTC_TIMESTAMP", "GET",
            "GET_JSON_OBJECT", "GETBIT", "GREATEST", "GROUPING", "GROUPING_ID", "H3_BOUNDARYASGEOJSON", "H3_BOUNDARYASWKB",
            "H3_BOUNDARYASWKT", "H3_CENTERASGEOJSON", "H3_CENTERASWKB", "H3_CENTERASWKT", "H3_COMPACT", "H3_DISTANCE", "H3_H3TOSTRING",
            "H3_HEXRING", "H3_ISCHILDOF", "H3_ISPENTAGON", "H3_ISVALID", "H3_KRING", "H3_KRINGDISTANCES", "H3_LONGLATASH3",
            "H3_LONGLATASH3STRING", "H3_MAXCHILD", "H3_MINCHILD", "H3_POINTASH3", "H3_POINTASH3STRING", "H3_POLYFILLASH3",
            "H3_POLYFILLASH3STRING", "H3_RESOLUTION", "H3_STRINGTOH3", "H3_TOCHILDREN", "H3_TOPARENT", "H3_TRY_POLYFILLASH3",
            "H3_TRY_POLYFILLASH3STRING", "H3_TRY_VALIDATE", "H3_UNCOMPACT", "H3_VALIDATE", "HASH", "HEX", "HOUR", "HYPOT", "IF", "IFF",
            "IFNULL", "IN", "INITCAP", "INLINE", "INLINE_OUTER", "INPUT_FILE_BLOCK_LENGTH", "INPUT_FILE_BLOCK_START", "INPUT_FILE_NAME",
            "INSTR", "INT", "IS_MEMBER", "ISNAN", "ISNOTNULL", "ISNULL", "JAVA_METHOD", "JSON_ARRAY_LENGTH", "JSON_OBJECT_KEYS",
            "JSON_TUPLE", "KURTOSIS", "LAG", "LAST", "LAST_DAY", "LAST_VALUE", "LCASE", "LEAD", "LEAST", "LEFT", "LEN", "LENGTH",
            "LEVENSHTEIN", "LIST_SECRETS", "LN", "LOCATE", "LOG", "LOG10", "LOG1P", "LOG2", "LOWER", "LPAD", "LTRIM", "MAKE_DATE",
            "MAKE_DT_INTERVAL", "MAKE_INTERVAL", "MAKE_TIMESTAMP", "MAKE_YM_INTERVAL", "MAP", "MAP_CONCAT", "MAP_CONTAINS_KEY",
            "MAP_ENTRIES", "MAP_FILTER", "MAP_FROM_ARRAYS", "MAP_FROM_ENTRIES", "MAP_KEYS", "MAP_VALUES", "MAP_ZIP_WITH", "MAX", "MAX_BY",
            "MD5", "MEAN", "MEDIAN", "MIN", "MIN_BY", "MINUTE", "MOD", "MODE", "MONOTONICALLY_INCREASING_ID", "MONTH", "MONTHS_BETWEEN",
            "NAMED_STRUCT", "NANVL", "NEGATIVE", "NEXT_DAY", "NOW", "NTH_VALUE", "NTILE", "NULLIF", "NVL", "NVL2", "OCTET_LENGTH",
            "OVERLAY", "PARSE_URL", "PERCENT_RANK", "PERCENTILE", "PERCENTILE_APPROX", "PERCENTILE_CONT", "PERCENTILE_DISC", "PI", "PMOD",
            "POSEXPLODE", "POSEXPLODE_OUTER", "POSITION", "POSITIVE", "POW", "POWER", "PRINTF", "QUARTER", "RADIANS", "RAISE_ERROR", "RAND",
            "RANDN", "RANDOM", "RANGE", "RANK", "REDUCE", "REFLECT", "REGEXP_COUNT", "REGEXP_EXTRACT", "REGEXP_EXTRACT_ALL", "REGEXP_INSTR",
            "REGEXP_LIKE", "REGEXP_REPLACE", "REGEXP_SUBSTR", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2",
            "REGR_SLOPE", "REGR_SXX", "REGR_SXY", "REGR_SYY", "REPEAT", "REPLACE", "REVERSE", "RIGHT", "RINT", "ROUND", "ROW_NUMBER",
            "RPAD", "RTRIM", "SCHEMA_OF_CSV", "SCHEMA_OF_JSON", "SEC", "SECOND", "SECRET", "SENTENCES", "SEQUENCE", "SHA", "SHA1", "SHA2",
            "SHIFTLEFT", "SHIFTRIGHT", "SHIFTRIGHTUNSIGNED", "SHUFFLE", "SIGN", "SIGNUM", "SIN", "SINH", "SIZE", "SKEWNESS", "SLICE",
            "SMALLINT", "SOME", "SORT_ARRAY", "SOUNDEX", "SPACE", "SPARK_PARTITION_ID", "SPLIT", "SPLIT_PART", "SQRT", "STACK",
            "STARTSWITH", "STD", "STDDEV", "STDDEV_POP", "STDDEV_SAMP", "STR_TO_MAP", "STRING", "STRUCT", "SUBSTR", "SUBSTRING",
            "SUBSTRING_INDEX", "SUM", "TABLE_CHANGES", "TAN", "TANH", "TIMESTAMP", "TIMESTAMP_MICROS", "TIMESTAMP_MILLIS",
            "TIMESTAMP_SECONDS", "TIMESTAMPADD", "TIMESTAMPDIFF", "TINYINT", "TO_BINARY", "TO_CHAR", "TO_CSV", "TO_DATE", "TO_JSON",
            "TO_NUMBER", "TO_TIMESTAMP", "TO_UNIX_TIMESTAMP", "TO_UTC_TIMESTAMP", "TRANSFORM", "TRANSFORM_KEYS", "TRANSFORM_VALUES",
            "TRANSLATE", "TRIM", "TRUNC", "TRY_ADD", "TRY_AVG", "TRY_CAST", "TRY_DIVIDE", "TRY_ELEMENT_AT", "TRY_MULTIPLY", "TRY_SUBTRACT",
            "TRY_SUM", "TRY_TO_BINARY", "TRY_TO_NUMBER", "TRY_TO_TIMESTAMP", "TYPEOF", "UCASE", "UNBASE64", "UNHEX", "UNIX_DATE",
            "UNIX_MICROS", "UNIX_MILLIS", "UNIX_SECONDS", "UNIX_TIMESTAMP", "UPPER", "URL_DECODE", "URL_ENCODE", "UUID", "VAR_POP",
            "VAR_SAMP", "VARIANCE", "VERSION", "WEEKDAY", "WEEKOFYEAR", "WIDTH_BUCKET", "WINDOW_TIME", "XPATH", "XPATH_BOOLEAN",
            "XPATH_DOUBLE", "XPATH_FLOAT", "XPATH_INT", "XPATH_LONG", "XPATH_NUMBER", "XPATH_SHORT", "XPATH_STRING", "XXHASH64", "YEAR",
            "ZIP_WITH"};

    /**
     * Initialize driver settings properly
     */
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addSQLKeywords(Arrays.asList(SPARK_EXTRA_KEYWORDS));
        addFunctions(Arrays.asList(SPARK_FUNCTIONS));
    }

    @Override
    public String[][] getIdentifierQuoteStrings() {
        return DEFAULT_QUOTE_STRINGS;
    }

    @Override
    public char getStringEscapeCharacter() {
        return '\\';
    }

    @Override
    public boolean supportsAlterTableStatement() {
        return false;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public String getColumnTypeModifiers(
        @NotNull DBPDataSource dataSource,
        @NotNull DBSTypedObject column,
        @NotNull String typeName,
        @NotNull DBPDataKind dataKind
    ) {
        switch (typeName) {
            case SQLConstants.DATA_TYPE_BIGINT:
            case SQLConstants.DATA_TYPE_BINARY:
            case SQLConstants.DATA_TYPE_BOOLEAN:
            case SQLConstants.DATA_TYPE_DOUBLE:
            case SQLConstants.DATA_TYPE_FLOAT:
            case SQLConstants.DATA_TYPE_INT:
            case SQLConstants.DATA_TYPE_SMALLINT:
            case SQLConstants.DATA_TYPE_STRING:
            case SQLConstants.DATA_TYPE_TINYINT:
                // These are data types without parameters
                return null;
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }
}
