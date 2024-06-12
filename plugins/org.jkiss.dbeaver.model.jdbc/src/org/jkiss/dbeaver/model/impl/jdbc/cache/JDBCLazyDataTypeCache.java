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

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;

import java.util.Collection;

public abstract class JDBCLazyDataTypeCache extends AbstractObjectCache<JDBCDataSource, DBSDataType> {
    protected final DBSObject owner;

    public JDBCLazyDataTypeCache(DBSObject owner)
    {
        this.owner = owner;
    }

    protected abstract DBSDataType createDataType(
        int valueType,
        String name,
        String remarks,
        boolean unsigned,
        boolean searchable,
        int precision,
        int minScale,
        int maxScale);

    @NotNull
    @Override
    public Collection<DBSDataType> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable JDBCDataSource jdbcDataSource) throws DBException
    {
        return getCachedObjects();
    }

    @Override
    public DBSDataType getObject(@NotNull DBRProgressMonitor monitor, @NotNull JDBCDataSource jdbcDataSource, @NotNull String name) throws DBException
    {
        return getCachedObject(name);
    }

    public DBSDataType getDataType(String name, int valueType)
    {
        return null;
    }

}
