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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectDDLExtension;
import org.jkiss.dbeaver.model.sql.SQLDialectSchemaController;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL dialect
 */
public class MySQLDialect extends JDBCSQLDialect implements SQLDialectSchemaController, SQLDialectDDLExtension {

    public static final String[] MYSQL_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "USE", "SHOW",
            "CREATE", "ALTER", "DROP",
            SQLConstants.KEYWORD_EXPLAIN, "DESCRIBE", "DESC"}
    );

    private static final String[] ADVANCED_KEYWORDS = {
        "AUTO_INCREMENT",
        "DATABASES",
        "COLUMNS",
        "ALGORITHM",
        "REPAIR",
        "ENGINE"
    };

    public static final String[][] MYSQL_QUOTE_STRINGS = {
        {"`", "`"},
        {"\"", "\""},
    };

    private static final String[] MYSQL_EXTRA_FUNCTIONS = {
        "ADDDATE",
        "ADDTIME",
        "ANY_VALUE",
        "CAST",
        "COALESCE",
        "COLLATION",
        "COMPRESS",
        "DATE_ADD",
        "DATE_SUB",
        "DATEDIFF",
        "EXTRACT",
        "FIRST_VALUE",
        "FORMAT",
        "FOUND_ROWS",
        "FROM_BASE64",
        "GET_FORMAT",
        "GROUP_CONCAT",
        "HOUR",
        "DAY",
        "IFNULL",
        "ISNULL",
        "LAG",
        "LAST_VALUE",
        "LEAD",
        "LEAST",
        "LENGTH",
        "MAKEDATE",
        "MAKETIME",
        "MINUTE",
        "MONTH",
        "NULLIF",
        "RANDOM_BYTES",
        "REPLACE",
        "REGEXP_LIKE",
        "REGEXP_INSTR",
        "REGEXP_REPLACE",
        "REGEXP_SUBSTR",
        "SESSION_USER",
        "SPACE",
        "SUBSTR",
        "SUBTIME",
        "TIMEDIFF",
        "TO_BASE64",
        "TO_SECONDS",
        "UUID",
        "UUID_TO_BIN",
        "WEEKOFYEAR",
        "YEAR"
    };

    private static final String[] MYSQL_GEOMETRY_FUNCTIONS = {
        "ST_ASWKT",
        "ST_GEOMCOLLFROMTEXT",
        "ST_GEOMETRYCOLLECTIONFROMTEXT",
        "ST_GEOMFROMTEXT",
        "ST_LINEFROMTEXT",
        "ST_MLINEFROMTEXT",
        "ST_MPOINTFROMTEXT",
        "ST_MPOLYFROMTEXT",
        "ST_POINTFROMTEXT",
        "ST_POLYFROMTEXT"
    };

    private static final String[] JSON_FUNCTIONS = {
        "JSON_ARRAY",
        "JSON_ARRAYAGG",
        "JSON_ARRAY_APPEND",
        "JSON_ARRAY_INSERT",
        "JSON_CONTAINS",
        "JSON_CONTAINS_PATH",
        "JSON_DEPTH",
        "JSON_EXTRACT",
        "JSON_INSERT",
        "JSON_KEYS",
        "JSON_LENGTH",
        "JSON_MERGE",
        "JSON_MERGE_PATCH",
        "JSON_MERGE_PRESERVE",
        "JSON_OBJECT",
        "JSON_OBJECTAGG",
        "JSON_QUOTE",
        "JSON_REMOVE",
        "JSON_REPLACE",
        "JSON_SEARCH",
        "JSON_SET",
        "JSON_TABLE",
        "JSON_TYPE",
        "JSON_UNQUOTE",
        "JSON_VALID",
        "JSON_VALUE"
    };

    private static final Pattern ONE_OR_MORE_DIGITS_PATTERN = Pattern.compile("[0-9]+");

    private static final String[] EXEC_KEYWORDS =  { "CALL" };
    private int lowerCaseTableNames;

    public MySQLDialect() {
        super("MySQL", "mysql");
    }

    public MySQLDialect(String name, String id) {
        super(name, id);
    }

    public void initBaseDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);

        addTableQueryKeywords(SQLConstants.KEYWORD_EXPLAIN, "DESCRIBE", "DESC");
        addFunctions(List.of("SLEEP"));

        addSQLKeywords(Arrays.asList(ADVANCED_KEYWORDS));
        removeSQLKeyword("SOURCE");

        // CHAR is data type, not function
        removeSQLKeyword("CHAR");

        addDataTypes(List.of("CHAR"));
        addFunctions(Arrays.asList(MYSQL_EXTRA_FUNCTIONS));
        addFunctions(Arrays.asList(JSON_FUNCTIONS));
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        initBaseDriverSettings(session, dataSource, metaData);

        addDataTypes(Arrays.asList("GEOMETRY", "POINT"));
        addFunctions(Arrays.asList(MYSQL_GEOMETRY_FUNCTIONS));
    }

    @Override
    public void afterDataSourceInitialization(@NotNull DBPDataSource dataSource) {
        this.lowerCaseTableNames = ((MySQLDataSource) dataSource).getLowerCaseTableNames();
        this.setSupportsUnquotedMixedCase(lowerCaseTableNames != 2);
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings() {
        return MYSQL_QUOTE_STRINGS;
    }

    @NotNull
    @Override
    public DBPIdentifierCase storesQuotedCase() {
        return DBPIdentifierCase.MIXED;
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
    public boolean mustBeQuoted(@NotNull String str, boolean forceCaseSensitive) {
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
            string = super.escapeString(string);
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

    @Override
    public boolean supportsUuid() {
        return false;
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
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, @NotNull String expression, boolean isInCondition) {
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

    @Override
    public EnumSet<ProjectionAliasVisibilityScope> getProjectionAliasVisibilityScope() {
        return EnumSet.of(
            ProjectionAliasVisibilityScope.GROUP_BY,
            ProjectionAliasVisibilityScope.HAVING,
            ProjectionAliasVisibilityScope.ORDER_BY
        );
    }

    @Nullable
    @Override
    public String getAutoIncrementKeyword() {
        return "AUTO_INCREMENT";
    }

    @Override
    public boolean supportsCreateIfExists() {
        return true;
    }

    @NotNull
    @Override
    public String getTimestampDataType() {
        return "TIMESTAMP";
    }

    @NotNull
    @Override
    public String getBigIntegerType() {
        return "BIGINT";
    }

    @NotNull
    @Override
    public String getClobDataType() {
        return "TEXT";
    }

    @NotNull
    @Override
    public String getBlobDataType() {
        return "BLOB";
    }

    @NotNull
    @Override
    public String getUuidDataType() {
        return "CHAR(36)";
    }

    @NotNull
    @Override
    public String getBooleanDataType() {
        return "TINYINT(1)";
    }

    @NotNull
    @Override
    public String getAlterColumnOperation() {
        return "MODIFY";
    }

    @Override
    public boolean supportsNoActionIndex() {
        return true;
    }

    @Override
    public boolean supportsAlterColumnSet() {
        return true;
    }

    @Override
    public boolean supportsAlterHasColumn() {
        return true;
    }
}
