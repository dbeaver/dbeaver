/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;

public class PolarDBXMySQLDataSourceInfo extends JDBCDataSourceInfo {

    private final PolarDBXMySQLDataSource dataSource;

    public PolarDBXMySQLDataSourceInfo(
        @NotNull PolarDBXMySQLDataSource dataSource,
        @NotNull JDBCDatabaseMetaData metaData
    ) {
        super(metaData);
        this.dataSource = dataSource;
    }

    @Override
    public boolean supportsMultipleResults() {
        return true;
    }

    @Override
    public boolean needsTableMetaForColumnResolution() {
        return true;
    }

    @NotNull
    @Override
    public String getDatabaseProductVersion() {
        return dataSource.getServerVersion();
    }

    /**
     * Get the database product name; the Standard Edition is shown as PolarDB-X (Standard Edition).
     */
    @NotNull
    @Override
    public String getDatabaseProductName() {
        String baseName = super.getDatabaseProductName();
        if (dataSource.isPolarDBXStandardEdition()) {
            return baseName + " (Standard Edition)";
        }
        return baseName;
    }
}
