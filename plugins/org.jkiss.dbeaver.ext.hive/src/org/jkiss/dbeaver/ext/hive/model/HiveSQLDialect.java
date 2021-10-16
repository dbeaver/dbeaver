/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hive.model;

import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;

import java.util.Arrays;

public class HiveSQLDialect extends GenericSQLDialect {

    private static final String[][] DEFAULT_QUOTE_STRINGS = {{"`", "`"}};

    public HiveSQLDialect() {
        super("HiveQL", "hive");
    }

    private static final String[] RESERVED_KEYWORDS = {
            "CONF",
            "EXCHANGE",
            "EXTENDED",
            "DATABASE",
            "IMPORT",
            "MACRO",
            "LESS",
            "PERCENT",
            "REDUCE",
            "TRUNCATE",
            "SHOW",
            "SYNC",
            "VIEWS"
    };

    private static final String[] NON_RESERVED_KEYWORDS = {
            "DEFAULT",
            "AFTER",
            "CASCADE",
            "DATA",
            "DEFINED",
            "INDEX",
            "ISOLATION",
            "KEY",
            "LEVEL",
            "LIMIT",
            "OPTION",
            "RENAME",
            "RESTRICT",
            "SCHEMA",
            "TRANSACTION",
            "VIEW"
    };

    private static final String[] HIVE_EXTRA_FUNCTIONS = {
            "ARRAY_CONTAINS",
            "COALESCE",
            "CURRENT_DATABASE",
            "DATE_ADD",
            "DATE_FORMAT",
            "DATEDIFF",
            "DAY",
            "DAYOFWEEK",
            "HOUR",
            "MINUTE",
            "MONTH",
            "RLIKE",
            "SECOND",
            "SIZE",
            "SUBSTRING_INDEX",
            "TO_DATE",
            "TRUNC",
            "VERSION",
            "YEAR",
            "WEEKOFYEAR"
    };

    private static final String[] HIVE_STRING_FUNCTIONS = {
            "ASCII",
            "CONCAT",
            "GET_JSON_OBJECT",
            "ENCODE",
            "LENGTH",
            "LPAD",
            "LTRIM",
            "REGEXP",
            "REGEXP_EXTRACT",
            "REGEXP_REPLACE",
            "REPLACE",
            "SUBSTR",
            "SUBSTRING",
            "QUOTE"
    };

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        for (String keyword : NON_RESERVED_KEYWORDS) {
            removeSQLKeyword(keyword);
        }
        addSQLKeywords(Arrays.asList(RESERVED_KEYWORDS));
        addFunctions(Arrays.asList(HIVE_EXTRA_FUNCTIONS));
        addFunctions(Arrays.asList(HIVE_STRING_FUNCTIONS));
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
}
