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
import org.jkiss.dbeaver.ext.mysql.model.MySQLDialect;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * StarRocks SQL Dialect - extends MySQL dialect.
 * StarRocks is always case-sensitive regardless of the lower_case_table_names setting.
 */
public class StarRocksDialect extends MySQLDialect {

    public StarRocksDialect() {
        super();
    }

    /**
     * Override to handle StarRocksDataSource instead of MySQLDataSource.
     * This prevents ClassCastException when parent="mysql" is set in plugin.xml.
     * StarRocks is always case-sensitive, so we always support unquoted mixed case.
     */
    @Override
    public void afterDataSourceInitialization(@NotNull DBPDataSource dataSource) {
        if (dataSource instanceof StarRocksDataSource) {
            // StarRocks is always case-sensitive - no need to check lower_case_table_names
            this.setSupportsUnquotedMixedCase(true);
        } else {
            super.afterDataSourceInitialization(dataSource);
        }
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
}
