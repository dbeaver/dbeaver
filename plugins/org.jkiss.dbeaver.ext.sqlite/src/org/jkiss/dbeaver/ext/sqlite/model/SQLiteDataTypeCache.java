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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteDataTypeCache
 */
public class SQLiteDataTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, SQLiteDataType>
{

    public SQLiteDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, GenericStructContainer dataSource) throws DBException {
        List<SQLiteDataType> types = new ArrayList<>();
        for (SQLiteAffinity affinity : SQLiteAffinity.values()) {
            SQLiteDataType dataType = new SQLiteDataType((SQLiteDataSource) dataSource, affinity);
            types.add(dataType);
        }
        setCache(types);
    }
}
