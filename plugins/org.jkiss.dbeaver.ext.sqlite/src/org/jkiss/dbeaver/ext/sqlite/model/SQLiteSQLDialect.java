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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class SQLiteSQLDialect extends GenericSQLDialect {

    public SQLiteSQLDialect() {
        super("SQLite", "sqlite");
        addKeywords(Set.of("STRICT"), DBPKeywordType.OTHER);
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (CommonUtils.isNaN(value) || CommonUtils.isInfinite(value)) {
            // SQLite doesn't have special literals for IEEE special values
            return "'" + value + "'";
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
    }

    @Override
    protected void loadFunctions(JDBCSession session, JDBCDatabaseMetaData metaData, Set<String> allFunctions) throws DBException, SQLException {
        allFunctions.addAll(List.of(
            "AVG",
            "COUNT",
            "MAX",
            "MIN",
            "SUM",
            "GROUP",
            "SUBSTR",
            "TRIM",
            "LTRIM",
            "RTRIM",
            "LENGTH",
            "REPLACE",
            "UPPER",
            "LOWER",
            "INSTR",
            "COALESCE",
            "IFNULL",
            "IIF",
            "NULLIF",
            "SQlite",
            "DATE",
            "TIME",
            "DATETIME",
            "JULIANDAY",
            "STRFTIME",
            "ABS",
            "RANDOM",
            "ROUND"
        ));
        super.loadFunctions(session, metaData, allFunctions);
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    public String[][] getIdentifierQuoteStrings() {
        return BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
    }

    @Override
    public boolean supportsAlterTableStatement() {
        return false;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode()
    {
        return MultiValueInsertMode.GROUP_ROWS;
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return true;
    }

    @Override
    public boolean supportsIndexCreateAndDrop() {
        return true;
    }

}
