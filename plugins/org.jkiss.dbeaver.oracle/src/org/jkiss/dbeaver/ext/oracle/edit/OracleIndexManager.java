/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;

import java.util.Collections;

/**
 * Oracle index manager
 */
public class OracleIndexManager extends JDBCIndexManager<OracleIndex, OracleTablePhysical> {

    protected OracleIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        DBECommandContext context, OracleTablePhysical parent,
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

        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName())).append("_")
            .append(CommonUtils.escapeIdentifier(editDialog.getSelectedColumns().iterator().next().getName()))
            .append("_IDX");
        final OracleIndex index = new OracleIndex(
            parent,
            DBObjectNameCaseTransformer.transformName((DBPDataSource) parent.getDataSource(), idxName.toString()),
            false,
            editDialog.getIndexType());
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            index.addColumn(
                new OracleIndexColumn(
                    index,
                    (OracleTableColumn) tableColumn,
                    colIndex++,
                    true));
        }
        return index;
    }

    protected String getDropIndexPattern(OracleIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT;
    }

}
