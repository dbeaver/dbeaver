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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.utils.CommonUtils;

/**
 * PostgreDataSourceInfo
 */
class PostgreDataSourceInfo extends JDBCDataSourceInfo {

    private final PostgreDataSource dataSource;

    public PostgreDataSourceInfo(PostgreDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super(metaData);
        this.dataSource = dataSource;
    }

    @Override
    public String getDatabaseProductVersion() {
        String serverVersion = dataSource.getServerVersion();
        return CommonUtils.isEmpty(serverVersion) ? super.getDatabaseProductVersion() : super.getDatabaseProductVersion() + "\n" + serverVersion;
    }

    @Override
    public boolean supportsMultipleResults() {
        return true;
    }

    @Override
    public boolean supportsDuplicateColumnsInResults() {
        return true;
    }

    @Override
    public boolean supportsResultSetLimit() {
        // ??? Disable maxRows for data transfer - it turns cursors off ?
        return dataSource.getServerType().supportsResultSetLimits();
    }

    @Override
    public boolean supportsTransactions() {
        return dataSource.getServerType().supportsTransactions();
    }

    @Override
    protected boolean isIgnoreReadOnlyFlag() {
        return true;
    }
}
