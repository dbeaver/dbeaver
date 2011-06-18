/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.model.MySQLConstraint;
import org.jkiss.dbeaver.ext.mysql.model.MySQLConstraintColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
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
            "Create constraint",
            parent,
            new DBSConstraintType[] {
                DBSConstraintType.PRIMARY_KEY,
                DBSConstraintType.UNIQUE_KEY });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final MySQLConstraint constraint = new MySQLConstraint(
            parent,
            null,
            null,
            editDialog.getConstraintType(),
            false);
        constraint.setName(JDBCObjectNameCaseTransformer.transformName(constraint, CommonUtils.escapeIdentifier(parent.getName()) + "_PK"));
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
        if (constraint.getConstraintType() == DBSConstraintType.PRIMARY_KEY) {
            clause = "PRIMARY KEY";
        } else {
            clause = "KEY";
        }
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT;
    }

}
