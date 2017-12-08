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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * Generic table manager
 */
public class GenericTableManager extends SQLTableManager<GenericTable, GenericStructContainer> {

    private static final Class<?>[] CHILD_TYPES = {
        GenericTableColumn.class,
        GenericPrimaryKey.class,
        GenericTableForeignKey.class,
        GenericTableIndex.class
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTable> getObjectsCache(GenericTable object)
    {
        return object.getContainer().getTableCache();
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    protected GenericTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, GenericStructContainer parent, Object copyFrom)
    {
        String tableName = "";
        try {
            tableName = getNewChildName(monitor, parent);
        } catch (DBException e) {
            log.error(e);
        }
        return new GenericTable(parent, tableName, "TABLE", null);
    }

    @Override
    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        // Filter out indexes for unique constraints (if they have the same name)
        DBPObject object = command.getObject();
        if (object instanceof DBSTableIndex) {
            for (NestedObjectCommand ccom : orderedCommands) {
                if (ccom.getObject() instanceof DBSEntityConstraint &&
                    ccom.getObject() != object &&
                    ((DBSEntityConstraint) ccom.getObject()).getConstraintType().isUnique() &&
                    CommonUtils.equalObjects(
                        ((DBSTableIndex) object).getName(), ((DBSEntityConstraint) ccom.getObject()).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

}
