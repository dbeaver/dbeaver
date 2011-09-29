/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.GenericIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericIndexColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;

import java.util.Collections;

/**
 * Generic index manager
 */
public class GenericIndexManager extends JDBCIndexManager<GenericIndex, GenericTable> {

    protected GenericIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        DBECommandContext context, GenericTable parent,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            workbenchWindow.getShell(),
            "Create index",
            parent,
            Collections.singletonList(DBSIndexType.OTHER));
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final GenericIndex index = new GenericIndex(
            parent,
            false,
            null,
            0,
            null,
            editDialog.getIndexType(),
            false);
        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
            }
            index.addColumn(
                new GenericIndexColumn(
                    index,
                    (GenericTableColumn) tableColumn,
                    colIndex++,
                    true));
        }
        idxName.append("_IDX");
        index.setName(DBObjectNameCaseTransformer.transformName(index, idxName.toString()));
        return index;
    }

}
