/*
 * Copyright (C) 2010-2014 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mssql.MSSQLMessages;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * MSSQL index manager
 */
public class MSSQLIndexManager extends JDBCIndexManager<MSSQLTableIndex, MSSQLTable> {

    @Override
    public DBSObjectCache<MSSQLCatalog, MSSQLTableIndex> getObjectsCache(MSSQLTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    protected MSSQLTableIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        DBECommandContext context, MSSQLTable parent,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            workbenchWindow.getShell(),
            MSSQLMessages.edit_index_manager_title,
            parent,
            Collections.singletonList(DBSIndexType.OTHER));
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final MSSQLTableIndex index = new MSSQLTableIndex(
            parent,
            false,
            null,
            editDialog.getIndexType(),
            null);
        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedColumns()) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName())); //$NON-NLS-1$
            }
            index.addColumn(
                new MSSQLTableIndexColumn(
                    index,
                    (MSSQLTableColumn) tableColumn,
                    colIndex++,
                    true,
                    false));
        }
        idxName.append("_IDX"); //$NON-NLS-1$
        index.setName(DBObjectNameCaseTransformer.transformName(index, idxName.toString()));
        return index;
    }

    @Override
    protected String getDropIndexPattern(MSSQLTableIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
