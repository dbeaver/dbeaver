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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Map;

/**
 * Generic index manager
 */
public class GenericIndexManager extends SQLIndexManager<GenericTableIndex, GenericTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableIndex> getObjectsCache(GenericTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return (container instanceof GenericTable)
            && ((GenericTable) container).getDataSource().getInfo().supportsIndexes()
            && ((GenericTable) container).getDataSource().getSQLDialect().supportsIndexCreateAndDrop();
    }

    @Override
    public boolean canEditObject(GenericTableIndex object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(@NotNull GenericTableIndex object) {
        return object.getDataSource().getSQLDialect().supportsIndexCreateAndDrop();
    }

    @Override
    protected GenericTableIndex createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container,
        Object from, @NotNull Map<String, Object> options)
    {
        GenericTableBase tableBase = (GenericTableBase) container;
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
