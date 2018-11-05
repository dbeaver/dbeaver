/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;

/**
 * PostgreDataSourceInfo
 */
class PostgreDataSourceInfo extends JDBCDataSourceInfo {

    private final boolean supportsLimits;

    public PostgreDataSourceInfo(PostgreDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super(metaData);
        supportsLimits = dataSource.getServerType().isSupportsLimits();
    }

    @Override
    public boolean supportsResultSetLimit() {
        // ??? Disable maxRows for data transfer - it turns cursors off ?
        return supportsLimits;
    }

    @Override
    protected boolean isIgnoreReadOnlyFlag() {
        return true;
    }
}
