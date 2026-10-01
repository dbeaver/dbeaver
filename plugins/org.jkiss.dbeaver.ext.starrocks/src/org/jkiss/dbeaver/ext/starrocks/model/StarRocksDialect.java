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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;

/**
 * StarRocks SQL Dialect - extends Generic SQL dialect.
 * StarRocks is always case-sensitive regardless of the lower_case_table_names setting.
 */
public class StarRocksDialect extends GenericSQLDialect {

    public StarRocksDialect() {
        super("StarRocks", "starrocks");
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        // StarRocks is always case-sensitive
        setSupportsUnquotedMixedCase(true);
    }

    /**
     * Override to handle StarRocksDataSource instead of MySQLDataSource.
     * StarRocks is always case-sensitive, so we always support unquoted mixed case.
     */
    @Override
    public void afterDataSourceInitialization(@NotNull DBPDataSource dataSource) {
        // StarRocks is always case-sensitive - no need to check lower_case_table_names
        this.setSupportsUnquotedMixedCase(true);
    }

    /**
     * StarRocks is always case-sensitive, so never use case-insensitive name lookup.
     */
    @Override
    public boolean useCaseInsensitiveNameLookup() {
        return false;
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "StarRocks";
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
     * StarRocks requires catalog names in all SQL statements.
     * The MySQL JDBC driver may report incorrect values, so we override.
     */
    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_ALL;
    }

    /**
     * StarRocks requires schema (database) names in all SQL statements.
     * This ensures 3-level FQN: catalog.database.table
     */
    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }
}
