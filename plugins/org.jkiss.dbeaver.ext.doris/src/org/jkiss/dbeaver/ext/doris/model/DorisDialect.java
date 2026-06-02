/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.doris.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;

/**
 * Doris SQL Dialect - extends Generic SQL dialect.
 */
public class DorisDialect extends GenericSQLDialect {

    public DorisDialect() {
        super("Apache Doris", "doris");
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        setSupportsUnquotedMixedCase(true);
    }

    @Override
    public void afterDataSourceInitialization(@NotNull DBPDataSource dataSource) {
        this.setSupportsUnquotedMixedCase(true);
    }

    @Override
    public boolean useCaseInsensitiveNameLookup() {
        return false;
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "Apache Doris";
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return new String[] { ";", "/" };
    }

    /**
     * Doris requires catalog names in all SQL statements.
     * The MySQL JDBC driver may report incorrect values, so we override.
     */
    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_ALL;
    }

    /**
     * Doris requires schema (database) names in all SQL statements.
     * This ensures 3-level FQN: catalog.database.table
     */
    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }
}
