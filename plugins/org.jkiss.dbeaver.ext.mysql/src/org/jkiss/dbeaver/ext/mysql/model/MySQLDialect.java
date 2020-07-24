/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;

/**
* MySQL dialect
*/
class MySQLDialect extends JDBCSQLDialect {

    public static final String[] MYSQL_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{
            "USE", "SHOW",
            "CREATE", "ALTER", "DROP",
            "EXPLAIN", "DESCRIBE", "DESC" }
    );

    public static final String[] ADVANCED_KEYWORDS = {
        "AUTO_INCREMENT",
        "DATABASES",
        "COLUMNS",
        "ALGORITHM",
        "REPAIR"
    };

    public static final String[][] MYSQL_QUOTE_STRINGS = {
            {"`", "`"},
            {"\"", "\""},
    };

    private static String[] EXEC_KEYWORDS =  { "CALL" };
    private int lowerCaseTableNames;

    public MySQLDialect() {
        super("MySQL");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        this.lowerCaseTableNames = ((MySQLDataSource)dataSource).getLowerCaseTableNames();
        this.setSupportsUnquotedMixedCase(lowerCaseTableNames != 2);

        //addSQLKeyword("STATISTICS");
        Collections.addAll(tableQueryWords, "EXPLAIN", "DESCRIBE", "DESC");
        addFunctions(Arrays.asList("SLEEP"));

        for (String kw : ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }
        removeSQLKeyword("SOURCE");

        addDataTypes(Arrays.asList("GEOMETRY", "POINT"));
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

    @NotNull
    @Override
    public String escapeString(String string) {
        return string.replace("'", "''").replace("\\", "\\\\");
    }

    @NotNull
    @Override
    public String unEscapeString(String string) {
        return string.replace("''", "'").replace("\\\\", "\\");
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
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
        return new String[] { "-- ", "#" };
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
    protected boolean useBracketsForExec() {
        return true;
    }

}
