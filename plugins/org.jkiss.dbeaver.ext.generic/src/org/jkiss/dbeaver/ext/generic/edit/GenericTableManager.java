/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
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
        final GenericTable table = new GenericTable(parent);
        try {
            setTableName(monitor, parent, table);
        } catch (DBException e) {
            log.error(e);
        }

        return table;
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
