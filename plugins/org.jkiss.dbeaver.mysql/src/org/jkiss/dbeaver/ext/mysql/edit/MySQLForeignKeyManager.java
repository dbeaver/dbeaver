/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCForeignKeyManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;

/**
 * Generic foreign manager
 */
public class MySQLForeignKeyManager extends JDBCForeignKeyManager<MySQLForeignKey, MySQLTable> {


    protected MySQLForeignKey createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, MySQLTable table, Object from)
    {
        EditForeignKeyDialog editDialog = new EditForeignKeyDialog(
            workbenchWindow.getShell(),
            "Create foreign key",
            activeEditor,
            table,
            new DBSConstraintModifyRule[] {
                DBSConstraintModifyRule.NO_ACTION,
                DBSConstraintModifyRule.CASCADE, DBSConstraintModifyRule.RESTRICT,
                DBSConstraintModifyRule.SET_NULL,
                DBSConstraintModifyRule.SET_DEFAULT });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final MySQLForeignKey foreignKey = new MySQLForeignKey(
            table,
            null,
            null,
            (MySQLConstraint) editDialog.getUniqueConstraint(),
            editDialog.getOnDeleteRule(),
            editDialog.getOnUpdateRule(),
            false);
        foreignKey.setName(JDBCObjectNameCaseTransformer.transformName(foreignKey,
            CommonUtils.escapeIdentifier(table.getName()) + "_" +
            CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getTable().getName()) + "_FK"));
        int colIndex = 1;
        for (EditForeignKeyDialog.FKColumnInfo tableColumn : editDialog.getColumns()) {
            foreignKey.addColumn(
                new MySQLForeignKeyColumn(
                    foreignKey,
                    (MySQLTableColumn) tableColumn.getOwnColumn(),
                    colIndex++,
                    (MySQLTableColumn) tableColumn.getRefColumn()));
        }
        return foreignKey;
    }

    protected String getDropForeignKeyPattern(MySQLForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP FOREIGN KEY " + PATTERN_ITEM_CONSTRAINT;
    }

}
