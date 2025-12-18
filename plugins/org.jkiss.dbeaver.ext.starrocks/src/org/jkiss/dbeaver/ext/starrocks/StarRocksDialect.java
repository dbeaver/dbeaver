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
package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDialect;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * StarRocks SQL Dialect - extends MySQL dialect.
 * Overrides afterDataSourceInitialization to handle StarRocksDataSource.
 */
public class StarRocksDialect extends MySQLDialect {

    public StarRocksDialect() {
        super();
    }

    /**
     * Override to handle StarRocksDataSource instead of MySQLDataSource.
     * This prevents ClassCastException when parent="mysql" is set in plugin.xml.
     */
    @Override
    public void afterDataSourceInitialization(@NotNull DBPDataSource dataSource) {
        if (dataSource instanceof StarRocksDataSource) {
            int lowerCaseTableNames = ((StarRocksDataSource) dataSource).getLowerCaseTableNames();
            this.setSupportsUnquotedMixedCase(lowerCaseTableNames != 2);
        } else {
            super.afterDataSourceInitialization(dataSource);
        }
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "StarRocks";
    }
}
