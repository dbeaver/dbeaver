/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2IndexColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexType;
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

import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Index manager
 * 
 * @author Denis Forveille
 * 
 */
public class DB2IndexManager extends SQLIndexManager<DB2Index, DB2Table> {

    private static final String CONS_IX_NAME = "%s_%s_IDX";

    private static final List<DBSIndexType> IX_TYPES;

    static {
        IX_TYPES = new ArrayList<DBSIndexType>(DB2IndexType.values().length);
        for (DB2IndexType db2IndexType : DB2IndexType.values()) {
            if (db2IndexType.isValidForCreation()) {
                IX_TYPES.add(db2IndexType.getDBSIndexType());
            }
        }
    }

    @Override
    public boolean canEditObject(DB2Index object)
    {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2Index> getObjectsCache(DB2Index object)
    {
        return object.getParentObject().getSchema().getIndexCache();
    }

    @Override
    protected DB2Index createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, DB2Table db2Table,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(workbenchWindow.getShell(),
            DB2Messages.edit_db2_index_manager_dialog_title, db2Table, IX_TYPES);
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        String tableName = CommonUtils.escapeIdentifier(db2Table.getName());
        String colName = CommonUtils.escapeIdentifier(editDialog.getSelectedAttributes().iterator().next().getName());

        String indexBaseName = String.format(CONS_IX_NAME, tableName, colName);
        String indexName = DBObjectNameCaseTransformer.transformName((DBPDataSource) db2Table.getDataSource(), indexBaseName);

        DB2Index index = new DB2Index(db2Table, indexName, editDialog.getIndexType());

        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            index.addColumn(new DB2IndexColumn(index, (DB2TableColumn) tableColumn, colIndex++));
        }
        return index;
    }

}
