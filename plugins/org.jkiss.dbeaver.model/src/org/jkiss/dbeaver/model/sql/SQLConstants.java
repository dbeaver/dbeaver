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

package org.jkiss.dbeaver.model.sql;

/**
 * SQL editor constants
 */
public class SQLConstants {

    public static final String NULL_VALUE = "NULL";

    public static final String STR_QUOTE_SINGLE = "'";
    public static final String STR_QUOTE_DOUBLE = "\"";
    public static final String[][] DOUBLE_QUOTE_STRINGS = {{ STR_QUOTE_DOUBLE, STR_QUOTE_DOUBLE }};

    public static final String DEFAULT_STATEMENT_DELIMITER = ";";
    public static final String[] DEFAULT_SCRIPT_DELIMITER = { DEFAULT_STATEMENT_DELIMITER };

    public static final String STR_QUOTE_APOS = "`";
    public static final String ML_COMMENT_START = "/*";
    public static final String ML_COMMENT_END = "*/";
    public static final String SL_COMMENT = "--";
    public static final String ASTERISK = "*";
    public static final String QUESTION = "?";
    public static final String DOT = ".";

    public static final String KEYWORD_SELECT = "SELECT";
    public static final String KEYWORD_INSERT = "INSERT";
    public static final String KEYWORD_UPDATE = "UPDATE";
    public static final String KEYWORD_DELETE = "DELETE";
    public static final String KEYWORD_MERGE = "MERGE";
    public static final String KEYWORD_UPSERT = "UPSERT";
    public static final String KEYWORD_TRUNCATE = "TRUNCATE";

    public static final String KEYWORD_FROM = "FROM";
    public static final String KEYWORD_INTO = "INTO";
    public static final String KEYWORD_JOIN = "JOIN";
    public static final String KEYWORD_CROSS_JOIN = "CROSS JOIN";
    public static final String KEYWORD_NATURAL_JOIN = "NATURAL JOIN";
    public static final String KEYWORD_WHERE = "WHERE";
    public static final String KEYWORD_SET = "SET";
    public static final String KEYWORD_ON = "ON";
    public static final String KEYWORD_AND = "AND";
    public static final String KEYWORD_OR = "OR";
    public static final String KEYWORD_BETWEEN = "BETWEEN";
    public static final String KEYWORD_IS = "IS";
    public static final String KEYWORD_NOT = "NOT";
    public static final String KEYWORD_NULL = "NULL";
    public static final String KEYWORD_IN = "IN";
    public static final String KEYWORD_VALUES = "VALUES";
    public static final String KEYWORD_ORDER_BY = "ORDER BY";
    public static final String KEYWORD_GROUP_BY = "GROUP BY";
    public static final String KEYWORD_HAVING = "HAVING";

    public static final String KEYWORD_LIKE = "LIKE";
    public static final String KEYWORD_ILIKE = "ILIKE";

    public static final String KEYWORD_FUNCTION = "FUNCTION";
    public static final String KEYWORD_PROCEDURE = "PROCEDURE";

    public static final String KEYWORD_COMMIT = "COMMIT";
    public static final String KEYWORD_ROLLBACK = "ROLLBACK";

    public static final String KEYWORD_EXPLAIN = "EXPLAIN";

    public static final String KEYWORD_CASE = "CASE";
    
    public static final String KEYWORD_QUALIFY = "QUALIFY";

    public static final String KEYWORD_AS = "AS";
    public static final String KEYWORD_USING = "USING";

    public static final String DATA_TYPE_VARCHAR = "varchar";
    public static final String DATA_TYPE_BIGINT = "BIGINT";
    public static final String DATA_TYPE_BINARY = "BINARY";
    public static final String DATA_TYPE_BOOLEAN = "BOOLEAN";
    public static final String DATA_TYPE_DOUBLE = "DOUBLE";
    public static final String DATA_TYPE_FLOAT = "FLOAT";
    public static final String DATA_TYPE_INT = "INT";
    public static final String DATA_TYPE_SMALLINT = "SMALLINT";
    public static final String DATA_TYPE_STRING = "STRING";
    public static final String DATA_TYPE_TINYINT = "TINYINT";

    public static final String[] QUERY_KEYWORDS = {
        KEYWORD_SELECT,
        KEYWORD_INSERT,
        KEYWORD_UPDATE,
        KEYWORD_DELETE,
        KEYWORD_MERGE,
        KEYWORD_UPSERT,
        KEYWORD_TRUNCATE
    };

    public static final String[] TABLE_KEYWORDS = {
        KEYWORD_FROM,
        KEYWORD_INSERT,
        KEYWORD_UPDATE,
        KEYWORD_DELETE,
        KEYWORD_INTO,
        "TABLE",
        "VIEW",
        KEYWORD_JOIN,
        KEYWORD_TRUNCATE,
        KEYWORD_MERGE,
    };

    public static final String[] COLUMN_KEYWORDS = {
        KEYWORD_SELECT,
        KEYWORD_WHERE,
        KEYWORD_SET,
        KEYWORD_ON,
        KEYWORD_AND,
        KEYWORD_OR,
        "BY",
        "HAVING"
    };

    public static final String[] DDL_KEYWORDS = {
        "CREATE",
        "ALTER",
        "DROP",
    };

    public static final String[] SQL2003_RESERVED_KEYWORDS = {
        "ALL",
        "ALLOCATE",
        "ALTER",
        KEYWORD_AND,
        "ANY",
        "ARE",
        "ARRAY",
        "AS",
        "ASENSITIVE",
        "ASYMMETRIC",
        "AT",
        "ATOMIC",
        "AUTHORIZATION",
        "BEGIN",
        KEYWORD_BETWEEN,
        //"BIGINT",
        DATA_TYPE_BINARY,
        "BOTH",
        "BY",
        "CALL",
        "CALLED",
        "CARDINALITY",
        "CASCADE",
        "CASCADED",
        KEYWORD_CASE,
        "CAST",
        "CEIL",
        "CEILING",
        "CHARACTER",
        "CHECK",
        "CLOSE",
        "COALESCE",
        "COLLATE",
        "COLLECT",
        "COLUMN",
        KEYWORD_COMMIT,
        "CONDITION",
        "CONNECT",
        "CONSTRAINT",
        "CONVERT",
        "CORR",
        "CORRESPONDING",
        "COVAR_POP",
        "COVAR_SAMP",
        "CREATE",
        "CROSS",
        "CUBE",
        "CUME_DIST",
        "CURRENT",
        "CURSOR",
        "CYCLE",
        "DAY",
        "DEALLOCATE",
        "DEC",
        "DECLARE",
        "DEFAULT",
        KEYWORD_DELETE,
        "DENSE_RANK",
        "DEREF",
        "DESCRIBE",
        "DETERMINISTIC",
        "DISCONNECT",
        "DISTINCT",
        "DROP",
        "DYNAMIC",
        "EACH",
        "ELEMENT",
        "ELSE",
        "END",
        "END-EXEC",
        "ESCAPE",
        "EVERY",
        "EXCEPT",
        "EXEC",
        "EXECUTE",
        "EXISTS",
        "EXP",
        "EXTERNAL",
        "EXTRACT",
        "FALSE",
        "FETCH",
        "FILTER",
        "FOR",
        "FOREIGN",
        "FREE",
        "FROM",
        "FULL",
        KEYWORD_FUNCTION,
        "FUSION",
        "GET",
        "GLOBAL",
        "GRANT",
        "GROUP",
        "GROUPING",
        "HAVING",
        "HOLD",
        "HOUR",
        "IDENTITY",
        "IF",
        KEYWORD_IN,
        "INDEX",
        "INDICATOR",
        "INNER",
        "INOUT",
        "INSENSITIVE",
        KEYWORD_INSERT,
        "INTERSECT",
        "INTERSECTION",
        "INTERVAL",
        "INTO",
        KEYWORD_IS,
        "JOIN",
        "LANGUAGE",
        "LARGE",
        "LATERAL",
        "LEADING",
        "LEFT",
        "LIKE",
        "LN",
        "LOCAL",
        "MATCH",
        "MEMBER",
        KEYWORD_MERGE,
        "METHOD",
        "MINUTE",
        "MOD",
        "MODIFIES",
//        "MODULE", // too common for column names
        "MONTH",
        "MULTISET",
        "NATIONAL",
        "NATURAL",
        //"NCHAR",
        //"NCLOB",
        "NEW",
        "NO",
        "NONE",
        "NORMALIZE",
        KEYWORD_NOT,
        KEYWORD_NULL,
        "NULLIF",
        "NUMERIC",
        "OF",
        "OLD",
        KEYWORD_ON,
        "ONLY",
        "OPEN",
        "OR",
        "ORDER",
        "OUT",
        "OUTER",
        "OVER",
        "OVERLAPS",
        "OVERLAY",
        "PARAMETER",
        "PARTITION",
        "POSITION",
        "PRECISION",
        "PREPARE",
        "PRIMARY",
        KEYWORD_PROCEDURE,
        "RANGE",
        "RANK",
        "READS",
        "REAL",
        "RECURSIVE",
        "REF",
        "REFERENCES",
        "REFERENCING",
        "RELEASE",
        "RENAME",
        "RESULT",
        "RETURN",
        "RETURNS",
        "REVOKE",
        "RIGHT",
        KEYWORD_ROLLBACK,
        "ROLLUP",
        "ROW",
        "ROW_NUMBER",
        "ROWS",
        "SAVEPOINT",
        "SCOPE",
        "SCROLL",
        "SEARCH",
        "SECOND",
        KEYWORD_SELECT,
        "SENSITIVE",
        "SESSION_USER",
        KEYWORD_SET,
        "SIMILAR",
        "SMALLINT",
        "SOME",
        "SPECIFIC",
        "SPECIFICTYPE",
        "SQL",
        "SQLEXCEPTION",
        "SQLSTATE",
        "SQLWARNING",
        "START",
        "STATIC",
//        "STDDEV_POP",
//        "STDDEV_SAMP",
        "SUBMULTISET",
        "SYMMETRIC",
        "SYSTEM",
        "SYSTEM_USER",
        "TABLE",
        "TABLESAMPLE",
        "THEN",
        "TIMEZONE_HOUR",
        "TIMEZONE_MINUTE",
        "TO",
        "TRAILING",
        "TRANSLATE",
        "TRANSLATION",
        "TREAT",
        "TRIGGER",
        "TRUE",
        "UNION",
        "UNIQUE",
        "UNKNOWN",
        "UNNEST",
        KEYWORD_UPDATE,
        "USER",
        "USING",
        //"VALUE", // too common for column names
        KEYWORD_VALUES,
//        "VAR_POP",
//        "VAR_SAMP",
        //"VARCHAR",
        "VARYING",
        "WHEN",
        "WHENEVER",
        KEYWORD_WHERE,
        "WIDTH_BUCKET",
        "WINDOW",
        "WITH",
        "WITHIN",
        "WITHOUT",
        "YEAR",

        "NULLS",
        "FIRST",
        "LAST",

        "FOLLOWING",
        "PRECEDING",
        "UNBOUNDED",

        "LENGTH",
        "KEY",
        "LEVEL",

        "VIEW",
        "SEQUENCE",
        "SCHEMA",
        "ROLE",
        "RESTRICT",
        "ASC",
        "DESC",

        // Not actually standard but widely used
        "LIMIT",

        // Extended keywords
//        "A",
        "ABSOLUTE",
        "ACTION",
//        "ADA",
        "ADD",
//        "ADMIN",
        "AFTER",
        "ALWAYS",
//        "ASC",
        "ASSERTION",
        "ASSIGNMENT",
        "ATTRIBUTE",
        "ATTRIBUTES",
        "BEFORE",
//        "BERNOULLI",
//        "BREADTH",
//        "C",
        "CASCADE",
        "CATALOG",
//        "CATALOG_NAME",
        "CHAIN",
//        "CHARACTER_SET_CATALOG",
//        "CHARACTER_SET_NAME",
//        "CHARACTER_SET_SCHEMA",
        "CHARACTERISTICS",
        "CHARACTERS",
//        "CLASS_ORIGIN",
//        "COBOL",
        "COLLATION",
//        "COLLATION_CATALOG",
//        "COLLATION_NAME",
//        "COLLATION_SCHEMA",
//        "COLUMN_NAME",
//        "COMMAND_FUNCTION",
//        "COMMAND_FUNCTION_CODE",
        "COMMITTED",
//        "CONDITION_NUMBER",
        "CONNECTION",
//        "CONNECTION_NAME",
//        "CONSTRAINT_CATALOG",
//        "CONSTRAINT_NAME",
//        "CONSTRAINT_SCHEMA",
        "CONSTRAINTS",
        "CONSTRUCTOR",
        "CONTAINS",
        "CONTINUE",
        "CURSOR_NAME",
        "DATA",
//        "DATETIME_INTERVAL_CODE",
//        "DATETIME_INTERVAL_PRECISION",
        "DEFAULTS",
        "DEFERRABLE",
        "DEFERRED",
        "DEFINED",
        "DEFINER",
        "DEGREE",
        "DEPTH",
        "DERIVED",
//        "DESC",
        "DESCRIPTOR",
        "DIAGNOSTICS",
        "DISPATCH",
        "DOMAIN",
//        "DYNAMIC_FUNCTION",
//        "DYNAMIC_FUNCTION_CODE",
        "EQUALS",
        "EXCEPTION",
        "EXCLUDE",
        "EXCLUDING",
        "FINAL",
        "FIRST",
//        "FORTRAN",
        "FOUND",
//        "G",
        "GENERAL",
        "GENERATED",
        "GO",
        "GOTO",
        "GRANTED",
        "HIERARCHY",
        "IMMEDIATE",
        "IMPLEMENTATION",
        "INCLUDING",
        "INCREMENT",
        "INITIALLY",
        "INPUT",
        "INSTANCE",
        "INSTANTIABLE",
        "INVOKER",
        "ISOLATION",
//        "K",
//        "KEY_MEMBER",
        "KEY_TYPE",
        "LAST",
        "LOCATOR",
//        "M",
        "MAP",
        "MATCHED",
        "MAXVALUE",
//        "MESSAGE_LENGTH",
//        "MESSAGE_OCTET_LENGTH",
//        "MESSAGE_TEXT",
        "MINVALUE",
        "MORE",
        "MUMPS",
//        "NAME",
//        "NAMES",
        "NESTING",
        "NEXT",
        "NORMALIZED",
//        "NULLABLE",
//        "NULLS",
//        "NUMBER",
        "OBJECT",
        "OCTETS",
        "OPTION",
        "OPTIONS",
        "ORDERING",
        "ORDINALITY",
        "OTHERS",
        "OUTPUT",
        "OVERRIDING",
        "PAD",
//        "PARAMETER_MODE",
//        "PARAMETER_NAME",
//        "PARAMETER_ORDINAL_POSITION",
//        "PARAMETER_SPECIFIC_CATALOG",
//        "PARAMETER_SPECIFIC_NAME",
//        "PARAMETER_SPECIFIC_SCHEMA",
        "PARTIAL",
//        "PASCAL",
        "PATH",
        "PLACING",
//        "PLI",
        "PRESERVE",
        "PRIOR",
        "PRIVILEGES",
//        "PUBLIC",
        "READ",
        "RELATIVE",
        "REPEATABLE",
        "RESTART",
//        "RETURNED_CARDINALITY",
//        "RETURNED_LENGTH",
//        "RETURNED_OCTET_LENGTH",
//        "RETURNED_SQLSTATE",
        "ROUTINE",
//        "ROUTINE_CATALOG",
//        "ROUTINE_NAME",
//        "ROUTINE_SCHEMA",
//        "ROW_COUNT",
        "SCALE",
//        "SCHEMA_NAME",
//        "SCOPE_CATALOG",
//        "SCOPE_NAME",
//        "SCOPE_SCHEMA",
        "SECTION",
        "SECURITY",
        "SELF",
        "SERIALIZABLE",
//        "SERVER_NAME",
        "SESSION",
        "SETS",
//        "SIMPLE",
        "SIZE",
        "SOURCE",
        "SPACE",
//        "SPECIFIC_NAME",
//        "STATE", // too common for column names
        "STATEMENT",
        "STRUCTURE",
        "STYLE",
//        "SUBCLASS_ORIGIN",
//        "TABLE_NAME",
        "TEMPORARY",
        "TIES",
//        "TOP_LEVEL_COUNT",
        "TRANSACTION",
//        "TRANSACTION_ACTIVE",
//        "TRANSACTIONS_COMMITTED",
//        "TRANSACTIONS_ROLLED_BACK",
        "TRANSFORM",
        "TRANSFORMS",
//        "TRIGGER_CATALOG",
//        "TRIGGER_NAME",
//        "TRIGGER_SCHEMA",
        "TYPE",
        "UNCOMMITTED",
        "UNDER",
        "UNNAMED",
        "USAGE",
//        "USER_DEFINED_TYPE_CATALOG",
//        "USER_DEFINED_TYPE_CODE",
//        "USER_DEFINED_TYPE_NAME",
//        "USER_DEFINED_TYPE_SCHEMA",
        "WORK",
        "WRITE",
        "ZONE",
        KEYWORD_QUALIFY
    };

    public static final String[] SQL2003_FUNCTIONS = {
        "ABS",
        "AVG",
        "CHAR_LENGTH",
        "CHARACTER_LENGTH",
        "COUNT",
        "LOCALTIME",
        "LOCALTIMESTAMP",
//        "CURRENT_DATE",
//        "CURRENT_DEFAULT_TRANSFORM_GROUP",
//        "CURRENT_PATH",
//        "CURRENT_ROLE",
//        "CURRENT_TIME",
//        "CURRENT_TIMESTAMP",
//        "CURRENT_TRANSFORM_GROUP_FOR_TYPE",
//        "CURRENT_USER",
        "FLOOR",
        "LOWER",
        "MAX",
        "MIN",
        "OCTET_LENGTH",
        "PERCENT_RANK",
        "PERCENTILE_CONT",
        "PERCENTILE_DISC",
        "POWER",
        "REGR_AVGX",
        "REGR_AVGY",
        "REGR_COUNT",
        "REGR_INTERCEPT",
        "REGR_R2",
        "REGR_SLOPE",
        "REGR_SXX",
        "REGR_SXY",
        "REGR_SYY",
        "SQRT",
        "SUBSTRING",
        "SUM",
        "TRIM",
        "UESCAPE",
        "UPPER",
    };

    public static final String[] SQL_EX_KEYWORDS = {
        "CHANGE",
        "MODIFY",
    };
    public static final String[] DEFAULT_TYPES = {
        DATA_TYPE_BOOLEAN,
        "CHAR",
        "VARCHAR",
        DATA_TYPE_BINARY,
        "VARBINARY",
        DATA_TYPE_INT,
        "INTEGER",
        DATA_TYPE_SMALLINT,
        DATA_TYPE_BIGINT,
        "NUMBER",
        "NUMERIC",
        "DECIMAL",
        DATA_TYPE_FLOAT,
        DATA_TYPE_DOUBLE,
        "DATE",
        "TIME",
        "TIMESTAMP",
        "CLOB",
        "BLOB",
    };

    public static final String BLOCK_BEGIN = "BEGIN";
    public static final String BLOCK_END = "END";

    /**
     * Pseudo variables - these are not dynamic parameters
     */
    public static final String[] PSEUDO_VARIABLES = {
        ":NEW",
        ":OLD",
    };

    public static final char STRUCT_SEPARATOR = '.'; //$NON-NLS-1$
    public static final String CONFIG_COLOR_KEYWORD = "org.jkiss.dbeaver.sql.editor.color.keyword.foreground";
    public static final String CONFIG_COLOR_DATATYPE = "org.jkiss.dbeaver.sql.editor.color.datatype.foreground";
    public static final String CONFIG_COLOR_FUNCTION = "org.jkiss.dbeaver.sql.editor.color.function.foreground";
    public static final String CONFIG_COLOR_STRING = "org.jkiss.dbeaver.sql.editor.color.string.foreground";
    public static final String CONFIG_COLOR_TABLE = "org.jkiss.dbeaver.sql.editor.color.table.foreground";
    public static final String CONFIG_COLOR_TABLE_ALIAS = "org.jkiss.dbeaver.sql.editor.color.table.alias.foreground";
    public static final String CONFIG_COLOR_COLUMN = "org.jkiss.dbeaver.sql.editor.color.column.foreground";
    public static final String CONFIG_COLOR_COLUMN_DERIVED = "org.jkiss.dbeaver.sql.editor.color.column.derived.foreground";
    public static final String CONFIG_COLOR_SCHEMA = "org.jkiss.dbeaver.sql.editor.color.schema.foreground";
    public static final String CONFIG_COLOR_COMPOSITE_FIELD = "org.jkiss.dbeaver.sql.editor.color.composite.field.foreground";
    public static final String CONFIG_COLOR_SQL_VARIABLE = "org.jkiss.dbeaver.sql.editor.color.sqlVariable.foreground";
    public static final String CONFIG_COLOR_SEMANTIC_ERROR = "org.jkiss.dbeaver.sql.editor.color.semanticError.foreground";
    public static final String CONFIG_COLOR_NUMBER = "org.jkiss.dbeaver.sql.editor.color.number.foreground";
    public static final String CONFIG_COLOR_COMMENT = "org.jkiss.dbeaver.sql.editor.color.comment.foreground";
    public static final String CONFIG_COLOR_DELIMITER = "org.jkiss.dbeaver.sql.editor.color.delimiter.foreground";
    public static final String CONFIG_COLOR_PARAMETER = "org.jkiss.dbeaver.sql.editor.color.parameter.foreground";
    public static final String CONFIG_COLOR_COMMAND = "org.jkiss.dbeaver.sql.editor.color.command.foreground";
    public static final String CONFIG_COLOR_TEXT = "org.jkiss.dbeaver.sql.editor.color.text.foreground";
    public static final String CONFIG_COLOR_BACKGROUND = "org.jkiss.dbeaver.sql.editor.color.text.background";
    public static final String CONFIG_COLOR_DISABLED = "org.jkiss.dbeaver.sql.editor.color.disabled.background";

    public static final char DEFAULT_PARAMETER_MARK = '?';
    public static final char DEFAULT_PARAMETER_PREFIX = ':';
    public static final String DEFAULT_IDENTIFIER_QUOTE = "\"";
    public static final String DEFAULT_LIKE_ESCAPE = "\\";
    public static final String KEYWORD_PATTERN_CHARS = "\\*\\";
    public static final String DEFAULT_CONTROL_COMMAND_PREFIX = "@";

    public final static char[] BRACKETS = {'{', '}', '(', ')', '[', ']', '<', '>'};
    public static final String COLUMN_ASTERISK = "*";

}
