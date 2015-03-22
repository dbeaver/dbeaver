/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.generic.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericPrimaryKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableConstraintColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
import org.jkiss.utils.CommonUtils;

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
        IWorkbenchWindow workbenchWindow,
        DBECommandContext context, GenericTable parent,
        Object from)
    {
        EditConstraintDialog editDialog = new EditConstraintDialog(
            workbenchWindow.getShell(),
            "Create constraint",
            parent,
            new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY} );
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final GenericPrimaryKey primaryKey = new GenericPrimaryKey(
            parent,
            null,
            null,
            editDialog.getConstraintType(),
            false);
        primaryKey.setName(DBObjectNameCaseTransformer.transformName(primaryKey, CommonUtils.escapeIdentifier(parent.getName()) + "_PK"));
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            primaryKey.addColumn(
                new GenericTableConstraintColumn(
                    primaryKey,
                    (GenericTableColumn) tableColumn,
                    colIndex++));
        }
        return primaryKey;
    }

}
