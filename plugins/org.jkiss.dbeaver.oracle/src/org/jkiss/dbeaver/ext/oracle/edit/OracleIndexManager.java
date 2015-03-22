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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndexColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablePhysical;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * Oracle index manager
 */
public class OracleIndexManager extends SQLIndexManager<OracleTableIndex, OracleTablePhysical> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableIndex> getObjectsCache(OracleTableIndex object)
    {
        return object.getParentObject().getSchema().indexCache;
    }

    @Override
    protected OracleTableIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        DBECommandContext context, OracleTablePhysical parent,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            workbenchWindow.getShell(),
            OracleMessages.edit_oracle_index_manager_dialog_title,
            parent,
            Collections.singletonList(DBSIndexType.OTHER));
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName())).append("_") //$NON-NLS-1$
            .append(CommonUtils.escapeIdentifier(editDialog.getSelectedAttributes().iterator().next().getName()))
            .append("_IDX"); //$NON-NLS-1$
        final OracleTableIndex index = new OracleTableIndex(
            parent.getSchema(),
            parent,
            DBObjectNameCaseTransformer.transformName((DBPDataSource) parent.getDataSource(), idxName.toString()),
            false,
            editDialog.getIndexType());
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            index.addColumn(
                new OracleTableIndexColumn(
                    index,
                    (OracleTableColumn) tableColumn,
                    colIndex++,
                    true));
        }
        return index;
    }

    @Override
    protected String getDropIndexPattern(OracleTableIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
