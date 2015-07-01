/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

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

    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    protected GenericTable createDatabaseObject(DBECommandContext context, GenericStructContainer parent, Object copyFrom)
    {
        final GenericTable table = new GenericTable(parent);
        try {
            setTableName(parent, table);
        } catch (DBException e) {
            log.error(e);
        }

        return table;
    }

    public DBEPersistAction[] getTableDDL(DBRProgressMonitor monitor, GenericTable table) throws DBException
    {
        GenericTableColumnManager tcm = new GenericTableColumnManager();
        GenericPrimaryKeyManager pkm = new GenericPrimaryKeyManager();
        GenericForeignKeyManager fkm = new GenericForeignKeyManager();
        GenericIndexManager im = new GenericIndexManager();

        StructCreateCommand command = makeCreateCommand(table);
        // Aggregate nested column, constraint and index commands
        for (GenericTableColumn column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
            command.aggregateCommand(tcm.makeCreateCommand(column));
        }
        try {
            for (GenericPrimaryKey primaryKey : CommonUtils.safeCollection(table.getConstraints(monitor))) {
                command.aggregateCommand(pkm.makeCreateCommand(primaryKey));
            }
        } catch (DBException e) {
            // Ignore primary keys
            log.debug(e);
        }
        try {
            for (GenericTableForeignKey foreignKey : CommonUtils.safeCollection(table.getAssociations(monitor))) {
                command.aggregateCommand(fkm.makeCreateCommand(foreignKey));
            }
        } catch (DBException e) {
            // Ignore primary keys
            log.debug(e);
        }
        try {
            for (GenericTableIndex index : CommonUtils.safeCollection(table.getIndexes(monitor))) {
                command.aggregateCommand(im.makeCreateCommand(index));
            }
        } catch (DBException e) {
            // Ignore indexes
            log.debug(e);
        }
        return command.getPersistActions();
    }

}
