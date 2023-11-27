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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectSchemaController;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL dialect
 */
public class MySQLDialect extends JDBCSQLDialect implements SQLDialectSchemaController {

    public static final String[] MYSQL_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
            BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS, new String[] { "USE", "SHOW", "CREATE", "ALTER", "DROP" });

    private static final String[] ADVANCED_KEYWORDS = { "AUTO_INCREMENT", "DATABASES", "COLUMNS", "ALGORITHM", "REPAIR",
            "ENGINE" };

    public static final String[][] MYSQL_QUOTE_STRINGS = { { "`", "`" }, { "\"", "\"" }, };

    // MySQL 8.2.0 types
    // See https://github.com/mysql/mysql-server/blob/mysql-8.2.0/sql/parse_tree_column_attrs.h
    private static final String[] MYSQL_DATA_TYPES = {
            // numeric types
            "NUMERIC", "REAL", "DOUBLE", "DECIMAL", "FIXED", "INTEGER", "INT", "INT1", "INT2", "INT3", "INT4",
            "TINYINT", "SMALLINT", "MEDIUMINT", "BIGINT",
            // BIT types
            "BIT",
            // BOOL types
            "BOOL", "BOOLEAN",
            // BLOB types
            "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "LONG", "LONG VARBINARY", "LONG VARCHAR", "TEXT", "TINYTEXT",
            "MEDIUMTEXT", "LONGTEXT",
            // time types
            "YEAR", "DATE", "TIME", "TIMESTAMP", "DATETIME", "TIMESTAMP",
            // ENUM types
            "ENUM",
            // SET types
            "SET",
            // JSON types
            "JSON", };

    // spatial types
    private static final String[] MYSQL_SPATIAL_DATA_TYPES = { "GEOMETRY", "GEOMCOLLECTION", "GEOMETRYCOLLECTION",
            "POINT", "MULTIPOINT", "LINESTRING", "MULTILINESTRING", "POLYGON", "MULTIPOLYGON", };

    // MySQL 8.2.0 reserved keywords
    // See https://dev.mysql.com/doc/mysqld-version-reference/en/keywords.html
    private static final String[] MYSQL_RESERVED_KEYWORDS = { "ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE", "AND",
            "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE",
            "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION", "CONSTRAINT", "CONTINUE",
            "CONVERT", "CREATE", "CROSS", "CUBE", "CUME_DIST", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "CURRENT_USER", "CURSOR", "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE",
            "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DENSE_RANK", "DESC", "DESCRIBE",
            "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE", "ELSEIF",
            "EMPTY", "ENCLOSED", "ESCAPED", "EXCEPT", "EXISTS", "EXIT", "EXPLAIN", "Yes", "FALSE", "FETCH",
            "FIRST_VALUE", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT", "FUNCTION",
            "GENERATED", "GET", "GRANT", "GROUP", "GROUPING", "GROUPS", "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND",
            "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE", "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE",
            "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8", "INTEGER", "INTERSECT", "INTERVAL", "INTO",
            "IO_AFTER_GTIDS", "IO_BEFORE_GTIDS", "IS", "ITERATE", "JOIN", "JSON_TABLE", "KEY", "KEYS", "KILL", "LAG",
            "LAST_VALUE", "LATERAL", "LEAD", "LEADING", "LEAVE", "LEFT", "LIKE", "LIMIT", "LINEAR", "LINES", "LOAD",
            "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY",
            "MASTER_BIND", "MASTER_SSL_VERIFY_SERVER_CERT", "es", "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT",
            "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL", "NOT",
            "NO_WRITE_TO_BINLOG", "NTH_VALUE", "NTILE", "NULL", "NUMERIC", "OF", "ON", "OPTIMIZE", "OPTIMIZER_COSTS",
            "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "OVER", "PARALLEL", "PARTITION",
            "PERCENT_RANK", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE", "RANGE", "RANK", "READ", "READS",
            "READ_WRITE", "REAL", "RECURSIVE", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT", "REPLACE",
            "REQUIRE", "RESIGNAL", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE", "ROW", "ROWS", "ROW_NUMBER",
            "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SIGNAL",
            "SMALLINT", "es", "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT",
            "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING", "STORED", "STRAIGHT_JOIN", "SYSTEM", "TABLE",
            "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER", "TRUE", "UNDO",
            "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME",
            "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING", "VIRTUAL", "WHEN", "WHERE",
            "WHILE", "WINDOW", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL", };

    // MySQL 8.2.0 Functions
    // This array has some duplicated item, but it's okay to register keyword.
    // See https://github.com/mysql/mysql-server/blob/mysql-8.2.0/plugin/x/src/mysql_function_names.cc
    private static final String[] MYSQL_EXTRA_FUNCTIONS = {
            // native_mysql_functions without geometry functions
            "ABS", "ACOS", "ADDTIME", "AES_DECRYPT", "AES_ENCRYPT", "ANY_VALUE", "ASIN", "ATAN", "ATAN2", "BENCHMARK",
            "BIN", "BIN_TO_UUID", "BIT_COUNT", "BIT_LENGTH", "CEIL", "CEILING", "CHARACTER_LENGTH", "CHAR_LENGTH",
            "COERCIBILITY", "COMPRESS", "CONCAT", "CONCAT_WS", "CONNECTION_ID", "CONV", "CONVERT_TZ", "COS", "COT",
            "CRC32", "CURRENT_ROLE", "DATEDIFF", "DATE_FORMAT", "DAYNAME", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR",
            "DEGREES", "ELT", "EXP", "EXPORT_SET", "EXTRACTVALUE", "FIELD", "FIND_IN_SET", "FLOOR", "FORMAT_BYTES",
            "FORMAT_PICO_TIME", "FOUND_ROWS", "FROM_BASE64", "FROM_DAYS", "FROM_UNIXTIME", "GET_LOCK", "GREATEST",
            "GTID_SUBSET", "GTID_SUBTRACT", "HEX", "ICU_VERSION", "IFNULL", "INET6_ATON", "INET6_NTOA", "INET_ATON",
            "INET_NTOA", "INSTR", "ISNULL", "IS_FREE_LOCK", "IS_IPV4", "IS_IPV4_COMPAT", "IS_IPV4_MAPPED", "IS_IPV6",
            "IS_USED_LOCK", "IS_UUID", "LAST_DAY", "LAST_INSERT_ID", "LCASE", "LEAST", "LENGTH", "LIKE_RANGE_MAX",
            "LIKE_RANGE_MIN", "LN", "LOAD_FILE", "LOCATE", "LOG", "LOG10", "LOG2", "LOWER", "LPAD", "LTRIM", "MAKEDATE",
            "MAKETIME", "MAKE_SET", "SOURCE_POS_WAIT", "MBRCONTAINS", "MBRCOVEREDBY", "MBRCOVERS", "MBRDISJOINT",
            "MBREQUALS", "MBRINTERSECTS", "MBROVERLAPS", "MBRTOUCHES", "MBRWITHIN", "MD5", "MONTHNAME", "NAME_CONST",
            "NULLIF", "OCT", "OCTET_LENGTH", "ORD", "PERIOD_ADD", "PERIOD_DIFF", "PI", "POW", "POWER",
            "PS_CURRENT_THREAD_ID", "PS_THREAD_ID", "QUOTE", "RADIANS", "RAND", "RANDOM_BYTES", "REGEXP_INSTR",
            "REGEXP_LIKE", "REGEXP_REPLACE", "REGEXP_SUBSTR", "RELEASE_ALL_LOCKS", "RELEASE_LOCK", "REVERSE",
            "ROLES_GRAPHML", "ROUND", "RPAD", "RTRIM", "SEC_TO_TIME", "SHA", "SHA1", "SHA2", "SIGN", "SIN", "SLEEP",
            "SOUNDEX", "SPACE", "SQRT", "STATEMENT_DIGEST", "STATEMENT_DIGEST_TEXT", "STRCMP", "STR_TO_DATE",
            "SUBSTRING_INDEX", "SUBTIME", "TAN", "TIMEDIFF", "TIME_FORMAT", "TIME_TO_SEC", "TO_BASE64", "TO_DAYS",
            "TO_SECONDS", "UCASE", "UNCOMPRESS", "UNCOMPRESSED_LENGTH", "UNHEX", "UNIX_TIMESTAMP", "UPDATEXML", "UPPER",
            "UUID", "UUID_SHORT", "UUID_TO_BIN", "VALIDATE_PASSWORD_STRENGTH", "VERSION", "WAIT_FOR_EXECUTED_GTID_SET",
            "WEEKDAY", "WEEKOFYEAR", "YEARWEEK",
            // special_mysql_functions
            "ADDDATE", "BIT_AND", "BIT_OR", "BIT_XOR", "CAST", "COUNT", "CURDATE", "CURTIME", "DATE_ADD", "DATE_SUB",
            "EXTRACT", "GROUP_CONCAT", "MAX", "MID", "MIN", "NOW", "POSITION", "SESSION_USER", "STD", "STDDEV",
            "STDDEV_POP", "STDDEV_SAMP", "SUBDATE", "SUBSTR", "SUBSTRING", "SUM", "SYSDATE", "SYSTEM_USER", "TRIM",
            "VARIANCE", "VAR_POP", "VAR_SAMP",
            // other_mysql_functions
            "ASCII", "AVG", "CHAR", "CHARSET", "COALESCE", "COLLATION", "CONVERT", "CUME_DIST", "CURDATE",
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURTIME", "DATABASE", "DATE",
            "DATE_ADD_INTERVAL", "DATE_SUB_INTERVAL", "DAY", "DEFAULT", "DENSE_RANK", "DISTINCT", "EXTRACT",
            "FIRST_VALUE", "FORMAT", "GEOMCOLLECTION", "GEOMETRYCOLLECTION", "GET_FORMAT", "GROUPING", "HOUR", "IF",
            "INSERT", "INTERVAL", "LAG", "LAST_VALUE", "LEAD", "LEFT", "LINESTRING", "LOCALTIME", "LOCALTIMESTAMP",
            "MICROSECOND", "MINUTE", "MOD", "MONTH", "MULTILINESTRING", "MULTIPOINT", "MULTIPOLYGON", "NTH_VALUE",
            "PASSWORD", "PERCENT_RANK", "POINT", "POLYGON", "POSITION", "QUARTER", "RANK", "REPEAT", "REPLACE",
            "REVERSE", "RIGHT", "ROW_COUNT", "ROW_NUMBER", "SCHEMA", "SECOND", "SUBDATE", "SUBSTRING", "SYSDATE",
            "TIME", "TIMESTAMP", "TIMESTAMPADD", "TIMESTAMPDIFF", "TRIM", "TRUNCATE", "USER", "UTC_DATE", "UTC_TIME",
            "UTC_TIMESTAMP", "VALUES", "WEEK", "WEIGHT_STRING", "YEAR", };

    // Geometry functions partial of native_mysql_functions
    private static final String[] MYSQL_GEOMETRY_FUNCTIONS = { "ST_AREA", "ST_ASBINARY", "ST_ASGEOJSON", "ST_ASTEXT",
            "ST_ASWKB", "ST_ASWKT", "ST_BUFFER", "ST_BUFFER_STRATEGY", "ST_CENTROID", "ST_CONTAINS", "ST_CONVEXHULL",
            "ST_CROSSES", "ST_DIFFERENCE", "ST_DIMENSION", "ST_DISJOINT", "ST_DISTANCE", "ST_DISTANCE_SPHERE",
            "ST_ENDPOINT", "ST_ENVELOPE", "ST_EQUALS", "ST_EXTERIORRING", "ST_FRECHETDISTANCE", "ST_GEOHASH",
            "ST_GEOMCOLLFROMTEXT", "ST_GEOMCOLLFROMTXT", "ST_GEOMCOLLFROMWKB", "ST_GEOMETRYCOLLECTIONFROMTEXT",
            "ST_GEOMETRYCOLLECTIONFROMWKB", "ST_GEOMETRYFROMTEXT", "ST_GEOMETRYFROMWKB", "ST_GEOMETRYN",
            "ST_GEOMETRYTYPE", "ST_GEOMFROMGEOJSON", "ST_GEOMFROMTEXT", "ST_GEOMFROMWKB", "ST_HAUSDORFFDISTANCE",
            "ST_INTERIORRINGN", "ST_INTERSECTION", "ST_INTERSECTS", "ST_ISCLOSED", "ST_ISEMPTY", "ST_ISSIMPLE",
            "ST_ISVALID", "ST_LATFROMGEOHASH", "ST_LATITUDE", "ST_LENGTH", "ST_LINEFROMTEXT", "ST_LINEFROMWKB",
            "ST_LINESTRINGFROMTEXT", "ST_LINESTRINGFROMWKB", "ST_LONGFROMGEOHASH", "ST_LONGITUDE", "ST_MAKEENVELOPE",
            "ST_MLINEFROMTEXT", "ST_MLINEFROMWKB", "ST_MPOINTFROMTEXT", "ST_MPOINTFROMWKB", "ST_MPOLYFROMTEXT",
            "ST_MPOLYFROMWKB", "ST_MULTILINESTRINGFROMTEXT", "ST_MULTILINESTRINGFROMWKB", "ST_MULTIPOINTFROMTEXT",
            "ST_MULTIPOINTFROMWKB", "ST_MULTIPOLYGONFROMTEXT", "ST_MULTIPOLYGONFROMWKB", "ST_NUMGEOMETRIES",
            "ST_NUMINTERIORRING", "ST_NUMINTERIORRINGS", "ST_NUMPOINTS", "ST_OVERLAPS", "ST_POINTFROMGEOHASH",
            "ST_POINTFROMTEXT", "ST_POINTFROMWKB", "ST_POINTN", "ST_POLYFROMTEXT", "ST_POLYFROMWKB",
            "ST_POLYGONFROMTEXT", "ST_POLYGONFROMWKB", "ST_SIMPLIFY", "ST_SRID", "ST_STARTPOINT", "ST_SWAPXY",
            "ST_SYMDIFFERENCE", "ST_TOUCHES", "ST_TRANSFORM", "ST_UNION", "ST_VALIDATE", "ST_WITHIN", "ST_X", "ST_Y", };

    // Json functions
    private static final String[] JSON_FUNCTIONS = {
            // mysql_functions_that_operate_on_json
            "JSON_CONTAINS", "JSON_CONTAINS_PATH", "JSON_DEPTH", "JSON_LENGTH", "JSON_OVERLAPS", "JSON_PRETTY",
            "JSON_SCHEMA_VALID", "JSON_SCHEMA_VALIDATION_REPORT", "JSON_STORAGE_FREE", "JSON_STORAGE_SIZE", "JSON_TYPE",
            "JSON_UNQUOTE", "JSON_VALID", "JSON_VALUE",
            // mysql_functions_that_return_json
            "JSON_ARRAY", "JSON_ARRAYAGG", "JSON_ARRAY_APPEND", "JSON_ARRAY_INSERT", "JSON_EXTRACT", "JSON_INSERT",
            "JSON_KEYS", "JSON_MERGE", "JSON_MERGE_PATCH", "JSON_MERGE_PRESERVE", "JSON_OBJECT", "JSON_OBJECTAGG",
            "JSON_QUOTE", "JSON_REMOVE", "JSON_REPLACE", "JSON_SCHEMA_VALIDATION_REPORT", "JSON_SEARCH", "JSON_SET", };

    private static final Pattern ONE_OR_MORE_DIGITS_PATTERN = Pattern.compile("[0-9]+");

    private static final String[] EXEC_KEYWORDS = { "CALL" };
    private int lowerCaseTableNames;

    public MySQLDialect() {
        super("MySQL", "mysql");
    }

    public MySQLDialect(String name, String id) {
        super(name, id);
    }

    public void initBaseDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        this.lowerCaseTableNames = ((MySQLDataSource) dataSource).getLowerCaseTableNames();
        this.setSupportsUnquotedMixedCase(lowerCaseTableNames != 2);

        clearAllKeywords();
        clearDataTypes();
        clearFunctions();
        clearReservedWords();

        addTableQueryKeywords(SQLConstants.KEYWORD_EXPLAIN, "DESCRIBE", "DESC");

        addFunctions(Arrays.asList(MYSQL_EXTRA_FUNCTIONS));
        addFunctions(Arrays.asList(JSON_FUNCTIONS));

        addDataTypes(Arrays.asList(MYSQL_DATA_TYPES));

        addSQLKeywords(Arrays.asList(ADVANCED_KEYWORDS));
        addSQLKeywords(Arrays.asList(MYSQL_RESERVED_KEYWORDS));
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        initBaseDriverSettings(session, dataSource, metaData);

        addDataTypes(Arrays.asList(MYSQL_SPATIAL_DATA_TYPES));
        addFunctions(Arrays.asList(MYSQL_GEOMETRY_FUNCTIONS));
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings() {
        return MYSQL_QUOTE_STRINGS;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @Override
    public char getStringEscapeCharacter() {
        return '\\';
    }

    @Nullable
    @Override
    public String getScriptDelimiterRedefiner() {
        return "DELIMITER";
    }

    @Override
    public String[][] getBlockBoundStrings() {
        // No anonymous blocks in MySQL
        return null;
    }

    @Override
    public boolean useCaseInsensitiveNameLookup() {
        return lowerCaseTableNames != 0;
    }

    @Override
    public boolean mustBeQuoted(String str, boolean forceCaseSensitive) {
        Matcher matcher = ONE_OR_MORE_DIGITS_PATTERN.matcher(str);
        if (matcher.lookingAt()) { // we should quote numeric names and names starts with number
            return true;
        }
        return super.mustBeQuoted(str, forceCaseSensitive);
    }

    @NotNull
    @Override
    protected String quoteIdentifier(@NotNull String str, @NotNull String[][] quoteStrings) {
        // Escape with first (default) quote string
        return quoteStrings[0][0] + escapeString(str, quoteStrings[0]) + quoteStrings[0][1];
    }

    @NotNull
    @Override
    public String escapeString(String string) {
        return escapeString(string, null);
    }

    @NotNull
    protected String escapeString(@NotNull String string, @Nullable String[] quotes) {
        if (quotes != null) {
            string = string.replace(quotes[0], quotes[0] + quotes[0]);
        } else {
            string = string.replace("'", "''").replace("`", "``");
        }

        return string.replaceAll("\\\\(?![_%?])", "\\\\\\\\");
    }

    @NotNull
    @Override
    public String unEscapeString(String string) {
        return string.replace("''", "'").replace("``", "`").replace("\\\\", "\\");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.GROUP_ROWS;
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

    @Override
    public String[] getSingleLineComments() {
        return new String[] { "-- ", "--\t", "#" };
    }

    @Override
    public String getTestSQL() {
        return "SELECT 1";
    }

    @NotNull
    public String[] getNonTransactionKeywords() {
        return MYSQL_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    public boolean isAmbiguousCountBroken() {
        return true;
    }

    @Override
    protected boolean useBracketsForExec(DBSProcedure procedure) {
        // Use brackets for CallableStatement. Support for procedures only
        return procedure.getProcedureType() == DBSProcedureType.PROCEDURE;
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (attribute.getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_JSON)) {
            return '\'' + escapeString(strValue) + '\'';
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }

    @Override
    public boolean validIdentifierStart(char c) {
        return Character.isLetterOrDigit(c);
    }

    @NotNull
    @Override
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, @NotNull String expression,
            boolean isInCondition) {
        if (isInCondition && attribute.getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_JSON)) {
            return "CAST(" + expression + " AS JSON)";
        } else {
            return super.getTypeCastClause(attribute, expression, isInCondition);
        }
    }

    @NotNull
    @Override
    public String getSchemaExistQuery(@NotNull String schemaName) {
        return "SHOW DATABASES LIKE " + getQuotedString(schemaName);
    }

    @NotNull
    @Override
    public String getCreateSchemaQuery(@NotNull String schemaName) {
        return "CREATE DATABASE " + schemaName;
    }
}
