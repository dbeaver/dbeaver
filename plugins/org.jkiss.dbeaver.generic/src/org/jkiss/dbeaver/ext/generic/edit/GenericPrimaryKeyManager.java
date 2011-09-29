/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.generic.model.GenericConstraintColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericPrimaryKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;

/**
 * Generic constraint manager
 */
public class GenericPrimaryKeyManager extends JDBCConstraintManager<GenericPrimaryKey, GenericTable> {

    protected GenericPrimaryKey createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor, DBECommandContext context, GenericTable parent,
        Object from)
    {
        EditConstraintDialog editDialog = new EditConstraintDialog(
            workbenchWindow.getShell(),
            "Create constraint",
            parent,
            new DBSConstraintType[] {DBSConstraintType.PRIMARY_KEY} );
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
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            primaryKey.addColumn(
                new GenericConstraintColumn(
                    primaryKey,
                    (GenericTableColumn) tableColumn,
                    colIndex++));
        }
        return primaryKey;
    }

}
