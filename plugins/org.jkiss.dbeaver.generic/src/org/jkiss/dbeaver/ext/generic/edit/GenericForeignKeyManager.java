/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IProgressControlProvider;
import org.jkiss.dbeaver.ext.generic.model.GenericForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericPrimaryKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCForeignKeyManager;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;

/**
 * Generic foreign manager
 */
public class GenericForeignKeyManager extends JDBCForeignKeyManager<GenericForeignKey, GenericPrimaryKey, GenericTable> {


    @Override
    protected GenericForeignKey createNewForeignKey(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, GenericTable table, Object from)
    {
        if (activeEditor instanceof MultiPageDatabaseEditor) {
            activeEditor = ((MultiPageDatabaseEditor) activeEditor).getActiveEditor();
        }
        EditForeignKeyDialog editDialog = new EditForeignKeyDialog(
            workbenchWindow.getShell(),
            "Create foreign key",
            activeEditor instanceof IProgressControlProvider ? (IProgressControlProvider) activeEditor : null,
            table);
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        return null;
//        final GenericForeignKey primaryKey = new GenericForeignKey(
//            table,
//            null,
//            null,
//            editDialog.getConstraintType(),
//            false);
//        primaryKey.setName(JDBCObjectNameCaseTransformer.transformName(primaryKey, CommonUtils.escapeIdentifier(parent.getName()) + "_PK"));
//        int colIndex = 1;
//        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
//            primaryKey.addColumn(
//                new GenericConstraintColumn(
//                    primaryKey,
//                    (GenericTableColumn) tableColumn,
//                    colIndex++));
//        }
//        return primaryKey;
    }

}
