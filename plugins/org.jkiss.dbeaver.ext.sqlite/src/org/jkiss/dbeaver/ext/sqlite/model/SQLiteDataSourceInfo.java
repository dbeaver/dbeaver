/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.ext.generic.model.GenericDataSourceInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;

public class SQLiteDataSourceInfo extends GenericDataSourceInfo {

    private final boolean isRemote;

    public SQLiteDataSourceInfo(DBPDriver driver, JDBCDatabaseMetaData metaData) {
        super(driver, metaData);
        this.isRemote = !driver.isEmbedded();
    }

    @Override
    public boolean supportsNullableUniqueConstraints() {
        return true;
    }

    // In LibSQL we don't have proper resulset metadata
    @Override
    public boolean needsTableMetaForColumnResolution() {
        return !isRemote;
    }
}
