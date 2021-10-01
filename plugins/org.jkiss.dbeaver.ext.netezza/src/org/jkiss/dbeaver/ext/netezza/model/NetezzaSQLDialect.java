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
package org.jkiss.dbeaver.ext.netezza.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Arrays;

public class NetezzaSQLDialect extends GenericSQLDialect {

    NetezzaSQLDialect() {
        super("Netezza", "netezza");
    }

    //Function without arguments/parameters
    private static final String[] OTHER_TYPES_FUNCTION = {
            "CURRENT_DB",
            "CURRENT_SID",
            "CURRENT_USERID",
            "CURRENT_USEROID",
            "CURRENT_CATALOG",
            "CURRENT_PATH",
            "CURRENT_SCHEMA",
            "CURRENT_DATE",
            "CURRENT_TIME",
            "CURRENT_TIMESTAMP",
            "CURRENT_TX_PATH",
            "CURRENT_TX_SCHEMA",
            "CURRENT_USER"
    };

    private static String[] NETEZZA_FUNCTIONS_DATETIME = new String[]{
            "DATE_PART",
            "DATE_TRUNC",
            "TIMEOFDAY"
    };

    private static String[] NETEZZA_FUNCTIONS = new String[]{
            "FIRST_VALUE",
            "LAG",
            "LAST_VALUE",
            "LEAD",
            "STDDEV",
            "STDDEV_POP",
            "STDDEV_SAMP",
            "VARIANCE",
            "VAR_POP",
            "VAR_SAMP"
    };

    private static String[] NETEZZA_KEYWORDS = {
            "ANALYZE",
            "COMMENT",
            "DECODE",
            "RESET",
            "DISTRIBUTE",
            "LOCK",
            "SHOW",
            "SYNONYM",
            "EXPRESS",
            "ONLINE",
            "RESET"
    };

    @Override
    public boolean validIdentifierStart(char c) {
        return super.validIdentifierStart(c) || c == '_';
    }

    @Override
    public boolean isCatalogAtStart() {
        return true;
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        if (typeName.equals("INTERVAL")) {
            return null;
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }

    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addKeywords(Arrays.asList(OTHER_TYPES_FUNCTION), DBPKeywordType.OTHER);
        addFunctions(Arrays.asList(NETEZZA_FUNCTIONS_DATETIME));
        addFunctions(Arrays.asList(NETEZZA_FUNCTIONS));
        addKeywords(Arrays.asList(NETEZZA_KEYWORDS), DBPKeywordType.KEYWORD);
    }
}
