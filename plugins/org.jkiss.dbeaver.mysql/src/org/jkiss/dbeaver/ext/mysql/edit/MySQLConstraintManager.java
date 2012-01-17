/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLConstraint;
import org.jkiss.dbeaver.ext.mysql.model.MySQLConstraintColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;

/**
 * MySQL constraint manager
 */
public class MySQLConstraintManager extends JDBCConstraintManager<MySQLConstraint, MySQLTable> {

    protected MySQLConstraint createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor, DBECommandContext context, MySQLTable parent,
        Object from)
    {
        EditConstraintDialog editDialog = new EditConstraintDialog(
            workbenchWindow.getShell(),
            MySQLMessages.edit_constraint_manager_title,
            parent,
            new DBSEntityConstraintType[] {
                DBSEntityConstraintType.PRIMARY_KEY,
                DBSEntityConstraintType.UNIQUE_KEY });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final MySQLConstraint constraint = new MySQLConstraint(
            parent,
            null,
            null,
            editDialog.getConstraintType(),
            false);
        constraint.setName(DBObjectNameCaseTransformer.transformName(constraint, CommonUtils.escapeIdentifier(parent.getName()) + "_PK")); //$NON-NLS-1$
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            constraint.addColumn(
                new MySQLConstraintColumn(
                    constraint,
                    (MySQLTableColumn) tableColumn,
                    colIndex++));
        }
        return constraint;
    }

    protected String getDropConstraintPattern(MySQLConstraint constraint)
    {
        String clause;
        if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
            clause = "PRIMARY KEY"; //$NON-NLS-1$
        } else {
            clause = "KEY"; //$NON-NLS-1$
        }
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
