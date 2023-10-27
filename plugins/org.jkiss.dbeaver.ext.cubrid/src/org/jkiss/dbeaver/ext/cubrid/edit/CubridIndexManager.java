/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableBase;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableIndex;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Map;

/**
 * Cubrid index manager
 */
public class CubridIndexManager extends SQLIndexManager<CubridTableIndex, CubridTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CubridTableIndex> getObjectsCache(CubridTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    public boolean canCreateObject(Object container) {
        return (container instanceof CubridTable)
            && ((CubridTable) container).getDataSource().getInfo().supportsIndexes()
            && ((CubridTable) container).getDataSource().getSQLDialect().supportsIndexCreateAndDrop();
    }

    @Override
    public boolean canEditObject(CubridTableIndex object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(CubridTableIndex object) {
        return object.getDataSource().getSQLDialect().supportsIndexCreateAndDrop();
    }

    @Override
    protected CubridTableIndex createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        CubridTableBase tableBase = (CubridTableBase) container;
        return tableBase.getDataSource().getMetaModel().createIndexImpl(
            tableBase,
            true,
            null,
            0,
            null,
            DBSIndexType.OTHER,
            false);
    }

}
