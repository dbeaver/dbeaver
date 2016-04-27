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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.*;
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
 * Postgre index manager
 */
public class PostgreIndexManager extends SQLIndexManager<PostgreIndex, PostgreTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreIndex> getObjectsCache(PostgreIndex object)
    {
        return object.getTable().getContainer().indexCache;
    }

    @Override
    protected PostgreIndex createDatabaseObject(
        DBECommandContext context, PostgreTableBase parent,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            DBeaverUI.getActiveWorkbenchShell(),
            "Edit index",
            parent,
            Collections.singletonList(DBSIndexType.OTHER));
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        final PostgreIndex index = new PostgreIndex(
            parent,
            idxName.toString(),
            editDialog.getIndexType(),
            editDialog.isUnique());
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName())); //$NON-NLS-1$
            }
            index.addColumn(
                new PostgreIndexColumn(
                    index,
                    (PostgreAttribute) tableColumn,
                    null,
                    colIndex++,
                    true,
                    false));
        }
        idxName.append("_IDX"); //$NON-NLS-1$
        index.setName(DBObjectNameCaseTransformer.transformObjectName(index, idxName.toString()));
        return index;
    }

    @Override
    protected String getDropIndexPattern(PostgreIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
