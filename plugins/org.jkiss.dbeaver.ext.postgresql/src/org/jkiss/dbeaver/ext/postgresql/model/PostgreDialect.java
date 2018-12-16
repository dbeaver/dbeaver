/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;

/**
* PostgreSQL dialect
*/
public class PostgreDialect extends JDBCSQLDialect {

    public static final String[] POSTGRE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "SHOW", "SET"
        }
    );

    /*

    We can use clean and short list of Postgre specific KeyWords
    and die trying to get one
    or we can just make some grouping like this
    https://docs.google.com/spreadsheets/d/1Bb9b52FjyWV49yOmoDT9C85cwIXoR-xrVVPfLwSIRYk/edit?usp=sharing

    and parse'em later against,
    the available list of native SQL KW from super class
    */

    public static final String[] POSTGRE_STARTING_COMMANDS = ArrayUtils.concatArrays(
        new String[]{
                "ABORT",
                "ALTER",
                "ANALYZE",
                "BEGIN",
                "CALL",
                "CLOSE",
                "CLUSTER",
                "COMMENT",
                "COMMIT",
                "COPY",
                "CREATE",
                "DEALLOCATE",
                "DECLARE",
                "DELETE",
                "DISCARD",
                "DO",
                "DROP",
                "END",
                "EXECUTE",
                "EXPLAIN",
                "FETCH",
                "GRANT",
                "IMPORT",
                "INSERT",
                "LISTEN",
                "LOAD",
                "LOCK",
                "MOVE",
                "NOTIFY",
                "PREPARE",
                "REFRESH",
                "REINDEX",
                "RELEASE",
                "RESET",
                "REVOKE",
                "ROLLBACK",
                "SAVEPOINT",
                "SECURITY",
                "SELECT",
                "SET",
                "SHOW",
                "START",
                "TRUNCATE",
                "UPDATE",
                "VALUES"
                },
        new String[]{ //those are some conditional, should check what to do with em
                "CHECKPOINT",
                "UNLISTEN",
                "VACUUM",
                "REASSIGN"
                }
    );

    // lots of duplicates atm, check for KW, DDL, commands and etc
    public static final String[] POSTGRE_COMMANDS = new String [] {
            "AGGREGATE",
            "COLLATION",
            "DATABASE",
            "DEFAULT",
            "PRIVILEGES",
            "DOMAIN",
            "TRIGGER",
            "EXTENSION",
            "FOREIGN",
            "TABLE",
            "FUNCTION",
            "GROUP",
            "LANGUAGE",
            "LARGE",
            "OBJECT",
            "MATERIALIZED",
            "VIEW",
            "OPERATOR",
            "CLASS",
            "FAMILY",
            "POLICY",
            "ROLE",
            "RULE",
            "SCHEMA",
            "SEQUENCE",
            "SERVER",
            "STATISTICS",
            "SUBSCRIPTION",
            "SYSTEM",
            "TABLESPACE",
            "CONFIGURATION",
            "DICTIONARY",
            "PARSER",
            "TEMPLATE",
            "TYPE",
            "USER",
            "MAPPING",
            "PREPARED",
            "ACCESS",
            "METHOD",
            "CAST",
            "AS",
            "TRANSFORM",
            "TRANSACTION",
            "OWNED",
            "TO",
            "INTO",
            "SESSION",
            "AUTHORIZATION",
            "INDEX",
            "PROCEDURE",
            "ASSERTION"
    };


    /**
     * Should exclude duplicates compared to {@link SQLConstants#DEFAULT_TYPES}
     *
     * Also exclude base data types from
     * @see PostgreConstants#DATA_TYPE_ALIASES
     * */
    public static final String[] POSTGRE_DATATYPES = new String[] {
            "BIGINT",
            "BIGSERIAL",
            "BIT",
            "BOOL",
            "BOOLEAN",
            "BOX",
            "BYTEA",
            "CHAR",
            "CHARACTER",
            "CIDR",
            "CIRCLE",
            "DATE",
            "DATERANGE",
            "DEC",
            "DECIMAL",
            "DOUBLE",
            "FLOAT", "FLOAT4", "FLOAT8",
            "INET",
            "INT", "INT2", "INT4", "INT8",
            "INT4RANGE", "INT8RANGE",
            "INTEGER",
            "INTERVAL",
            "JSON",
            "JSONB",
            "LINE",
            "LSEG",
            "MACADDR", "MACADDR8",
            "MONEY",
            "NATIONAL",
            "NCHAR",
            "NUMERIC",
            "NUMRANGE",
            "PATH",
            "POINT",
            "POLYGON",
            "PRECISION",
            "REAL",
            "SERIAL", "SERIAL2", "SERIAL4", "SERIAL8",
            "SMALLINT",
            "SMALLSERIAL",
            "TEXT",
            "TIME",
            "TIMESTAMP",
            "TIMESTAMPTZ",
            "TIMETZ",
            "TSQUERY",
            "TSRANGE",
            "TSTZRANGE",
            "TSVECTOR",
            "TXID_SNAPSHOT",
            "UUID",
            "VARBIT",
            "VARCHAR",
            "VARYING",
            "XML",
            "ZONE"
    };

    public static final String[] POSTGRE_EXCEPTIONS = new String[]{/* din't look if there is a common place for this type of tings*/};

    //region FUNCTION KeyWords
    public static String[] POSTGRE_FUNCTIONS_AGGR = new String[]{
            "ARRAY_AGG",
            "AVG",
            "BIT_AND",
            "BIT_OR",
            "BOOL_AND",
            "BOOL_OR",
            "COUNT",
            "EVERY",
            "JSON_AGG",
            "JSONB_AGG",
            "JSON_OBJECT_AGG",
            "JSONB_OBJECT_AGG",
            "MAX",
            "MIN",
            "MODE",
            "STRING_AGG",
            "SUM",
            "XMLAGG",
            "CORR",
            "COVAR_POP",
            "COVAR_SAMP",
            "REGR_AVGX",
            "REGR_AVGY",
            "REGR_COUNT",
            "REGR_INTERCEPT",
            "REGR_R2",
            "REGR_SLOPE",
            "REGR_SXX",
            "REGR_SXY",
            "REGR_SYY",
            "STDDEV",
            "STDDEV_POP",
            "STDDEV_SAMP",
            "VARIANCE",
            "VAR_POP",
            "VAR_SAMP",
            "PERCENTILE_CONT",
            "PERCENTILE_DISC"
    };

    public static String[] POSTGRE_FUNCTIONS_WINDOW = new String[]{
            "ROW_NUMBER",
            "RANK",
            "DENSE_RANK",
            "PERCENT_RANK",
            "CUME_DIST",
            "NTILE",
            "LAG",
            "LEAD",
            "FIRST_VALUE",
            "LAST_VALUE",
            "NTH_VALUE"
    };


    public static String[] POSTGRE_FUNCTIONS_MATH = new String[]{
            "ABS",
            "CBRT",
            "CEIL",
            "CEILING",
            "DEGREES",
            "DIV",
            "EXP",
            "FLOOR",
            "LN",
            "LOG",
            "MOD",
            "PI",
            "POWER",
            "RADIANS",
            "ROUND",
            "SCALE",
            "SIGN",
            "SQRT",
            "TRUNC",
            "WIDTH_BUCKET",
            "RANDOM",
            "SETSEED",
            "ACOS",
            "ACOSD",
            "ASIN",
            "ASIND",
            "ATAN",
            "ATAND",
            "ATAN2",
            "ATAN2D",
            "COS",
            "COSD",
            "COT",
            "COTD",
            "SIN",
            "SIND",
            "TAN",
            "TAND"
    };
    public static String[] POSTGRE_FUNCTIONS_STRING = new String[]{
            "BIT_LENGTH",
            "CHAR_LENGTH",
            "CHARACTER_LENGTH",
            "LOWER",
            "OCTET_LENGTH",
            "OVERLAY",
            "POSITION",
            "SUBSTRING",
            "TREAT",
            "TRIM",
            "UPPER",
            "ASCII",
            "BTRIM",
            "CHR",
            "CONCAT",
            "CONCAT_WS",
            "CONVERT",
            "CONVERT_FROM",
            "CONVERT_TO",
            "DECODE",
            "ENCODE",
            "INITCAP",
            "LEFT",
            "LENGTH",
            "LPAD",
            "LTRIM",
            "MD5",
            "PARSE_IDENT",
            "PG_CLIENT_ENCODING",
            "QUOTE_IDENT",
            "QUOTE_LITERAL",
            "QUOTE_NULLABLE",
            "REGEXP_MATCH",
            "REGEXP_MATCHES",
            "REGEXP_REPLACE",
            "REGEXP_SPLIT_TO_ARRAY",
            "REGEXP_SPLIT_TO_TABLE",
            "REPEAT",
            "REPLACE",
            "REVERSE",
            "RIGHT",
            "RPAD",
            "RTRIM",
            "SPLIT_PART",
            "STRPOS",
            "SUBSTR",
            "TO_ASCII",
            "TO_HEX",
            "TRANSLATE"
    };

    public static String[] POSTGRE_FUNCTIONS_DATETIME = new String[]{
            "AGE",
            "CLOCK_TIMESTAMP",
            "DATE_PART",
            "DATE_TRUNC",
            "ISFINITE",
            "JUSTIFY_DAYS",
            "JUSTIFY_HOURS",
            "JUSTIFY_INTERVAL",
            "MAKE_DATE",
            "MAKE_INTERVAL",
            "MAKE_TIME",
            "MAKE_TIMESTAMP",
            "MAKE_TIMESTAMPTZ",
            "NOW",
            "STATEMENT_TIMESTAMP",
            "TIMEOFDAY",
            "TRANSACTION_TIMESTAMP"
    };

    public static String[] POSTGRE_FUNCTIONS_GEOMETRY = new String[]{
            "AREA",
            "CENTER",
            "DIAMETER",
            "HEIGHT",
            "ISCLOSED",
            "ISOPEN",
            "NPOINTS",
            "PCLOSE",
            "POPEN",
            "RADIUS",
            "WIDTH",
            "BOX",
            "BOUND_BOX",
            "CIRCLE",
            "LINE",
            "LSEG",
            "PATH",
            "POLYGON"
    };

    public static String[] POSTGRE_FUNCTIONS_NETWROK = new String[]{
            "ABBREV",
            "BROADCAST",
            "HOST",
            "HOSTMASK",
            "MASKLEN",
            "NETMASK",
            "NETWORK",
            "SET_MASKLEN",
            "TEXT",
            "INET_SAME_FAMILY",
            "INET_MERGE",
            "MACADDR8_SET7BIT"
    };

    public static String[] POSTGRE_FUNCTIONS_LO = new String[]{
            "LO_FROM_BYTEA",
            "LO_PUT",
            "LO_GET",
            "LO_CREAT",
            "LO_CREATE",
            "LO_UNLINK",
            "LO_IMPORT",
            "LO_EXPORT",
            "LOREAD",
            "LOWRITE",
            "GROUPING",
            "CAST"
    };

    public static String[] POSTGRE_FUNCTIONS_ADMIN = new String[]{
            "CURRENT_SETTING",
            "SET_CONFIG",
            "BRIN_SUMMARIZE_NEW_VALUES",
            "BRIN_SUMMARIZE_RANGE",
            "BRIN_DESUMMARIZE_RANGE",
            "GIN_CLEAN_PENDING_LIST"
    };

    public static String[] POSTGRE_FUNCTIONS_RANGE = new String[]{
            "ISEMPTY",
            "LOWER_INC",
            "UPPER_INC",
            "LOWER_INF",
            "UPPER_INF",
            "RANGE_MERGE"
    };

    public static String[] POSTGRE_FUNCTIONS_TEXT_SEARCH = new String[]{
            "ARRAY_TO_TSVECTOR",
            "GET_CURRENT_TS_CONFIG",
            "NUMNODE",
            "PLAINTO_TSQUERY",
            "PHRASETO_TSQUERY",
            "WEBSEARCH_TO_TSQUERY",
            "QUERYTREE",
            "SETWEIGHT",
            "STRIP",
            "TO_TSQUERY",
            "TO_TSVECTOR",
            "JSON_TO_TSVECTOR",
            "JSONB_TO_TSVECTOR",
            "TS_DELETE",
            "TS_FILTER",
            "TS_HEADLINE",
            "TS_RANK",
            "TS_RANK_CD",
            "TS_REWRITE",
            "TSQUERY_PHRASE",
            "TSVECTOR_TO_ARRAY",
            "TSVECTOR_UPDATE_TRIGGER",
            "TSVECTOR_UPDATE_TRIGGER_COLUMN"
    };

    public static String[] POSTGRE_FUNCTIONS_XML = new String[]{
            "XMLCOMMENT",
            "XMLCONCAT",
            "XMLELEMENT",
            "XMLFOREST",
            "XMLPI",
            "XMLROOT",
            "XMLEXISTS",
            "XML_IS_WELL_FORMED",
            "XML_IS_WELL_FORMED_DOCUMENT",
            "XML_IS_WELL_FORMED_CONTENT",
            "XPATH",
            "XPATH_EXISTS",
            "XMLTABLE",
            "XMLNAMESPACES",
            "TABLE_TO_XML",
            "TABLE_TO_XMLSCHEMA",
            "TABLE_TO_XML_AND_XMLSCHEMA",
            "QUERY_TO_XML",
            "QUERY_TO_XMLSCHEMA",
            "QUERY_TO_XML_AND_XMLSCHEMA",
            "CURSOR_TO_XML",
            "CURSOR_TO_XMLSCHEMA",
            "SCHEMA_TO_XML",
            "SCHEMA_TO_XMLSCHEMA",
            "SCHEMA_TO_XML_AND_XMLSCHEMA",
            "DATABASE_TO_XML",
            "DATABASE_TO_XMLSCHEMA",
            "DATABASE_TO_XML_AND_XMLSCHEMA",
            "XMLATTRIBUTES"
    };

    public static String[] POSTGRE_FUNCTIONS_JSON = new String[]{
            "TO_JSON",
            "TO_JSONB",
            "ARRAY_TO_JSON",
            "ROW_TO_JSON",
            "JSON_BUILD_ARRAY",
            "JSONB_BUILD_ARRAY",
            "JSON_BUILD_OBJECT",
            "JSONB_BUILD_OBJECT",
            "JSON_OBJECT",
            "JSONB_OBJECT",
            "JSON_ARRAY_LENGTH",
            "JSONB_ARRAY_LENGTH",
            "JSON_EACH",
            "JSONB_EACH",
            "JSON_EACH_TEXT",
            "JSONB_EACH_TEXT",
            "JSON_EXTRACT_PATH",
            "JSONB_EXTRACT_PATH",
            "JSON_OBJECT_KEYS",
            "JSONB_OBJECT_KEYS",
            "JSON_POPULATE_RECORD",
            "JSONB_POPULATE_RECORD",
            "JSON_POPULATE_RECORDSET",
            "JSONB_POPULATE_RECORDSET",
            "JSON_ARRAY_ELEMENTS",
            "JSONB_ARRAY_ELEMENTS",
            "JSON_ARRAY_ELEMENTS_TEXT",
            "JSONB_ARRAY_ELEMENTS_TEXT",
            "JSON_TYPEOF",
            "JSONB_TYPEOF",
            "JSON_TO_RECORD",
            "JSONB_TO_RECORD",
            "JSON_TO_RECORDSET",
            "JSONB_TO_RECORDSET",
            "JSON_STRIP_NULLS",
            "JSONB_STRIP_NULLS",
            "JSONB_SET",
            "JSONB_INSERT",
            "JSONB_PRETTY"
    };

    public static String[] POSTGRE_FUNCTIONS_ARRAY = new String[]{
            "ARRAY_APPEND",
            "ARRAY_CAT",
            "ARRAY_NDIMS",
            "ARRAY_DIMS",
            "ARRAY_FILL",
            "ARRAY_LENGTH",
            "ARRAY_LOWER",
            "ARRAY_POSITION",
            "ARRAY_POSITIONS",
            "ARRAY_PREPEND",
            "ARRAY_REMOVE",
            "ARRAY_REPLACE",
            "ARRAY_TO_STRING",
            "ARRAY_UPPER",
            "CARDINALITY",
            "STRING_TO_ARRAY",
            "UNNEST"
    };

    public static String[] POSTGRE_FUNCTIONS_INFO = new String[]{
            "CURRENT_DATABASE",
            "CURRENT_QUERY",
            "CURRENT_SCHEMA",
            "CURRENT_SCHEMAS",
            "INET_CLIENT_ADDR",
            "INET_CLIENT_PORT",
            "INET_SERVER_ADDR",
            "INET_SERVER_PORT",
            "ROW_SECURITY_ACTIVE",
            "FORMAT_TYPE",
            "TO_REGCLASS",
            "TO_REGPROC",
            "TO_REGPROCEDURE",
            "TO_REGOPER",
            "TO_REGOPERATOR",
            "TO_REGTYPE",
            "TO_REGNAMESPACE",
            "TO_REGROLE",
            "COL_DESCRIPTION",
            "OBJ_DESCRIPTION",
            "SHOBJ_DESCRIPTION",
            "TXID_CURRENT",
            "TXID_CURRENT_IF_ASSIGNED",
            "TXID_CURRENT_SNAPSHOT",
            "TXID_SNAPSHOT_XIP",
            "TXID_SNAPSHOT_XMAX",
            "TXID_SNAPSHOT_XMIN",
            "TXID_VISIBLE_IN_SNAPSHOT",
            "TXID_STATUS"
    };

    public static String[] POSTGRE_FUNCTIONS_COMPRASION = new String[]{
            "NUM_NONNULLS",
            "NUM_NULLS"
    };

    public static String[] POSTGRE_FUNCTIONS_FORMATTING = new String[]{
            "TO_CHAR",
            "TO_DATE",
            "TO_NUMBER",
            "TO_TIMESTAMP"
    };

    public static String[] POSTGRE_FUNCTIONS_ENUM = new String[]{
            "ENUM_FIRST",
            "ENUM_LAST",
            "ENUM_RANGE"
    };

    public static String[] POSTGRE_FUNCTIONS_SEQUENCE = new String[]{
            "CURRVAL",
            "LASTVAL",
            "NEXTVAL",
            "SETVAL"
    };

    public static String[] POSTGRE_FUNCTIONS_BINARY_STRING = new String[]{
            "OCTET_LENGTH",
            "GET_BIT",
            "GET_BYTE",
            "SET_BIT",
            "SET_BYTE"
    };

    public static String[] POSTGRE_FUNCTIONS_CONDITIONAL= new String[]{
            "COALESCE",
            "NULLIF",
            "GREATEST",
            "LEAST"
    };

    public static String[] POSTGRE_FUNCTIONS_TRIGGER= new String[]{
            "SUPPRESS_REDUNDANT_UPDATES_TRIGGER"
    };

    public static String[] POSTGRE_FUNCTIONS_SRF = new String[]{
            "GENERATE_SERIES",
            "GENERATE_SUBSCRIPTS"
    };
    //endregion

    //region Reserverd, RoleAttrs, and Clause KeyWords

    public static final String[] POSTGRE_ROLE_ATTRS = new String[]{
            "SUPERUSER",
            "NOSUPERUSER",
            "CREATEDB",
            "NOCREATEDB",
            "CREATEROLE",
            "NOCREATEROLE",
            "INHERIT",
            "NOINHERIT",
            "LOGIN",
            "NOLOGIN",
            "REPLICATION",
            "NOREPLICATION",
            "BYPASSRLS",
            "NOBYPASSRLS"
    };

    public static final String[] POSTGRE_KW_SET_2 = new String[]{
            "ALIAS",
            "BEGIN",
            "CONSTANT",
            "DECLARE",
            "END",
            "EXCEPTION",
            "RETURN",
            "PERFORM ",
            "RAISE",
            "GET",
            "DIAGNOSTICS",
            "STACKED ",
            "FOREACH",
            "LOOP",
            "ELSIF",
            "EXIT",
            "WHILE",
            "REVERSE",
            "SLICE",
            "DEBUG",
            "LOG",
            "INFO",
            "NOTICE",
            "WARNING",
            "ASSERT",
            "OPEN"
    };

    public static final String[] POSTGRE_CLAUSE_KW = new String[]{
            "BY",
            "RETURNS",
            "INOUT",
            "OUT",
            "SETOF",
            "IF",
            "STRICT",
            "CURRENT",
            "CONTINUE",
            "OWNER",
            "LOCATION",
            "OVER",
            "PARTITION",
            "WITHIN",
            "BETWEEN",
            "ESCAPE",
            "EXTERNAL",
            "INVOKER",
            "DEFINER",
            "WORK",
            "RENAME",
            "VERSION",
            "CONNECTION",
            "CONNECT",
            "TABLES",
            "TEMP",
            "TEMPORARY",
            "FUNCTIONS",
            "SEQUENCES",
            "TYPES",
            "SCHEMAS",
            "OPTION",
            "CASCADE",
            "RESTRICT",
            "ADD",
            "ADMIN",
            "EXISTS",
            "VALID",
            "VALIDATE",
            "ENABLE",
            "DISABLE",
            "REPLICA",
            "ALWAYS",
            "PASSING",
            "COLUMNS",
            "PATH",
            "REF",
            "VALUE",
            "OVERRIDING",
            "IMMUTABLE",
            "STABLE",
            "VOLATILE",
            "BEFORE",
            "AFTER",
            "EACH",
            "ROW",
            "PROCEDURAL",
            "ROUTINE",
            "NO",
            "HANDLER",
            "VALIDATOR",
            "OPTIONS",
            "STORAGE",
            "OIDS",
            "WITHOUT",
            "INHERIT",
            "DEPENDS",
            "CALLED",
            "INPUT",
            "LEAKPROOF",
            "COST",
            "ROWS",
            "NOWAIT",
            "SEARCH",
            "UNTIL",
            "ENCRYPTED",
            "PASSWORD",
            "CONFLICT",
            "INSTEAD",
            "INHERITS",
            "CHARACTERISTICS",
            "WRITE",
            "CURSOR",
            "ALSO",
            "STATEMENT",
            "SHARE",
            "EXCLUSIVE",
            "INLINE",
            "ISOLATION",
            "REPEATABLE",
            "READ",
            "COMMITTED",
            "SERIALIZABLE",
            "UNCOMMITTED",
            "LOCAL",
            "GLOBAL",
            "SQL",
            "PROCEDURES",
            "RECURSIVE",
            "SNAPSHOT",
            "ROLLUP",
            "CUBE",
            "TRUSTED",
            "INCLUDE",
            "FOLLOWING",
            "PRECEDING",
            "UNBOUNDED",
            "RANGE",
            "GROUPS",
            "UNENCRYPTED",
            "SYSID",
            "FORMAT",
            "DELIMITER",
            "HEADER",
            "QUOTE",
            "ENCODING",
            "FILTER",
            "OFF"
    };

    public static final String[] POSTGRE_RESERVED_KW = new String[]{
            "ALL",
            "ANALYSE",
            "AND",
            "ANY",
            "ARRAY",
            "ASC",
            "ASYMMETRIC",
            "BOTH",
            "CASE",
            "CHECK",
            "COLLATE",
            "COLUMN",
            "CONCURRENTLY",
            "CONSTRAINT",
            "CROSS",
            "DEFERRABLE",
            "RANGE",
            "DESC",
            "DISTINCT",
            "ELSE",
            "EXCEPT",
            "FOR",
            "FREEZE",
            "FROM",
            "FULL",
            "HAVING",
            "ILIKE",
            "IN",
            "INITIALLY",
            "INNER",
            "INTERSECT",
            "IS",
            "ISNULL",
            "JOIN",
            "LATERAL",
            "LEADING",
            "LIKE",
            "LIMIT",
            "NATURAL",
            "NOT",
            "NOTNULL",
            "NULL",
            "OFFSET",
            "ON",
            "ONLY",
            "OR",
            "ORDER",
            "OUTER",
            "OVERLAPS",
            "PLACING",
            "PRIMARY",
            "REFERENCES",
            "RETURNING",
            "SIMILAR",
            "SOME",
            "SYMMETRIC",
            "TABLESAMPLE",
            "THEN",
            "TRAILING",
            "UNION",
            "UNIQUE",
            "USING",
            "VARIADIC",
            "VERBOSE",
            "WHEN",
            "WHERE",
            "WINDOW",
            "WITH"
    };

    //endregion

    public PostgreDialect() {
        super("PostgreSQL");
    }

    public void addExtraKeywords(String ... keywords) {
        super.addSQLKeywords(Arrays.asList(keywords));
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);

        addExtraKeywords(
            "SHOW",
            "TYPE",
            "USER",
            "COMMENT",
            "MATERIALIZED",
            "ILIKE",
            "ELSIF",
            "ELSEIF",
            "ANALYSE",
            "ANALYZE",
            "CONCURRENTLY",
            "FREEZE",
            "LANGUAGE",
            "MODULE",
            "OFFSET",
            //"PUBLIC",
            "RETURNING",
            "VARIADIC",
            "PERFORM",
            "FOREACH",
            "LOOP",
            "PERFORM",
            "RAISE",
            "NOTICE",
            "CONFLICT",
            "EXTENSION",

            // "DEBUG", "INFO", "NOTICE", "WARNING", // levels
            // "MESSAGE", "DETAIL", "HINT", "ERRCODE", //options

            "DATATYPE",
            "TABLESPACE",
            "REFRESH"
        );

        addExtraKeywords(
            "CURRENT_DATABASE",
            "ARRAY_AGG",
            "BIT_AND",
            "BIT_OR",
            "BOOL_AND",
            "BOOL_OR",
            "JSON_AGG",
            "JSONB_AGG",
            "JSON_OBJECT_AGG",
            "JSONB_OBJECT_AGG",
            "STRING_AGG",
            "XMLAGG",
            "BIT_LENGTH",
            "CURRENT_CATALOG",
            "CURRENT_SCHEMA",
            "SQLCODE",
            "LENGTH",
            "SQLERROR"
        );

        addFunctions(Arrays.asList(PostgreConstants.POSTGIS_FUNCTIONS));

        removeSQLKeyword("LENGTH");

        if (dataSource instanceof PostgreDataSource) {
            ((PostgreDataSource) dataSource).getServerType().configureDialect(this);
        }
    }

    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_NONE;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @Nullable
    @Override
    public String getBlockToggleString() {
        return "$" + SQLConstants.KEYWORD_PATTERN_CHARS + "$";
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        // PostgreSQL-specific blocks ($$) should be used everywhere
        return null;//super.getBlockBoundStrings();
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @Override
    public boolean supportsCommentQuery() {
        return true;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return PostgreBinaryFormatter.INSTANCE;
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(PostgreConstants.DATA_TYPE_ALIASES.keySet());
    }

    @NotNull
    @Override
    protected String[] getNonTransactionKeywords() {
        return POSTGRE_NON_TRANSACTIONAL_KEYWORDS;
    }
}
