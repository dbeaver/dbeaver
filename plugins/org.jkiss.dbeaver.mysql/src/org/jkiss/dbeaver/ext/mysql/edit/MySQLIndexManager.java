/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * MySQL index manager
 */
public class MySQLIndexManager extends SQLIndexManager<MySQLTableIndex, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableIndex> getObjectsCache(MySQLTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    protected MySQLTableIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        DBECommandContext context, MySQLTable parent,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            workbenchWindow.getShell(),
            MySQLMessages.edit_index_manager_title,
            parent,
            Collections.singletonList(DBSIndexType.OTHER));
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final MySQLTableIndex index = new MySQLTableIndex(
            parent,
            false,
            null,
            editDialog.getIndexType(),
            null);
        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName())); //$NON-NLS-1$
            }
            index.addColumn(
                new MySQLTableIndexColumn(
                    index,
                    (MySQLTableColumn) tableColumn,
                    colIndex++,
                    true,
                    false));
        }
        idxName.append("_IDX"); //$NON-NLS-1$
        index.setName(DBObjectNameCaseTransformer.transformName(index, idxName.toString()));
        return index;
    }

    @Override
    protected String getDropIndexPattern(MySQLTableIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
