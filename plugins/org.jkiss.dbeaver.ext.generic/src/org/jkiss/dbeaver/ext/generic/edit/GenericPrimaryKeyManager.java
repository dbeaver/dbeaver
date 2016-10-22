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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * Generic constraint manager
 */
public class GenericPrimaryKeyManager extends SQLConstraintManager<GenericPrimaryKey, GenericTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericPrimaryKey> getObjectsCache(GenericPrimaryKey object)
    {
        return object.getParentObject().getContainer().getPrimaryKeysCache();
    }

    @Override
    protected GenericPrimaryKey createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final GenericTable parent,
        Object from)
    {
        return new UITask<GenericPrimaryKey>() {
            @Override
            protected GenericPrimaryKey runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    "Create constraint",
                    parent,
                    new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY} );
                if (!editPage.edit()) {
                    return null;
                }

                final GenericPrimaryKey primaryKey = new GenericPrimaryKey(
                    parent,
                    null,
                    null,
                    editPage.getConstraintType(),
                    false);
                primaryKey.setName(editPage.getConstraintName());
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    primaryKey.addColumn(
                        new GenericTableConstraintColumn(
                            primaryKey,
                            (GenericTableColumn) tableColumn,
                            colIndex++));
                }
                return primaryKey;
            }
        }.execute();
    }

    @Override
    protected boolean isLegacyConstraintsSyntax(GenericTable owner) {
        return ((GenericSQLDialect)owner.getDataSource().getSQLDialect()).isLegacySQLDialect();
    }
}
