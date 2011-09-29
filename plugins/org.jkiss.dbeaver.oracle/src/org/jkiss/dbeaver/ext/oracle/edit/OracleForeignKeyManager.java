/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCForeignKeyManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;
import org.jkiss.dbeaver.ui.dialogs.struct.EditForeignKeyDialog;

/**
 * Oracle foreign key manager
 */
public class OracleForeignKeyManager extends JDBCForeignKeyManager<OracleForeignKey, OracleTableBase> {


    protected OracleForeignKey createDatabaseObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext context, OracleTableBase table, Object from)
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

        final OracleForeignKey foreignKey = new OracleForeignKey(
            table,
            null,
            null,
            (OracleConstraint) editDialog.getUniqueConstraint(),
            editDialog.getOnDeleteRule());
        foreignKey.setName(DBObjectNameCaseTransformer.transformName(foreignKey,
                CommonUtils.escapeIdentifier(table.getName()) + "_" +
                        CommonUtils.escapeIdentifier(editDialog.getUniqueConstraint().getTable().getName()) + "_FK"));
        int colIndex = 1;
        for (EditForeignKeyDialog.FKColumnInfo tableColumn : editDialog.getColumns()) {
            foreignKey.addColumn(
                new OracleForeignKeyColumn(
                    foreignKey,
                    (OracleTableColumn) tableColumn.getOwnColumn(),
                    colIndex++));
        }
        return foreignKey;
    }

    protected String getDropForeignKeyPattern(OracleForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP FOREIGN KEY " + PATTERN_ITEM_CONSTRAINT;
    }

}
