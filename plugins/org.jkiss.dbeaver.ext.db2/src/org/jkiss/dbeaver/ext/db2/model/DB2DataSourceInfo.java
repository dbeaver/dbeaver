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

package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * DB2 data source info
 */
class DB2DataSourceInfo extends JDBCDataSourceInfo {

    public DB2DataSourceInfo(JDBCDatabaseMetaData metaData) {
        super(metaData);
    }

    @Override
    public boolean supportsMultipleResults() {
        return true;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes() {
        return DB2ObjectType.values();
    }
}
