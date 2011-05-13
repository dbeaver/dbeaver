/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.model.MySQLIndex;
import org.jkiss.dbeaver.ext.mysql.model.MySQLIndexColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;

import java.util.Collections;

/**
 * MySQL index manager
 */
public class MySQLIndexManager extends JDBCIndexManager<MySQLIndex, MySQLTable> {

    protected MySQLIndex createNewIndex(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor, MySQLTable parent,
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

        final MySQLIndex index = new MySQLIndex(
            parent,
            false,
            null,
            editDialog.getIndexType(),
            null);
        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
            }
            index.addColumn(
                new MySQLIndexColumn(
                    index,
                    (MySQLTableColumn) tableColumn,
                    colIndex++,
                    true,
                    false));
        }
        idxName.append("_IDX");
        index.setName(JDBCObjectNameCaseTransformer.transformName(index, idxName.toString()));
        return index;
    }

    protected String getDropIndexPattern(MySQLIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT;
    }

}
