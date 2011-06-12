/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.oracle.model.OracleIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleIndexColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;

import java.util.Collections;

/**
 * Oracle index manager
 */
public class OracleIndexManager extends JDBCIndexManager<OracleIndex, OracleTable> {

    protected OracleIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        DBECommandContext context, OracleTable parent,
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

        final OracleIndex index = new OracleIndex(
            parent,
            false,
            null,
            editDialog.getIndexType());
        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            if (colIndex == 1) {
                idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
            }
            index.addColumn(
                new OracleIndexColumn(
                    index,
                    (OracleTableColumn) tableColumn,
                    colIndex++,
                    true));
        }
        idxName.append("_IDX");
        index.setName(JDBCObjectNameCaseTransformer.transformName(index, idxName.toString()));
        return index;
    }

    protected String getDropIndexPattern(OracleIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT;
    }

}
