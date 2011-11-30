/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;

/**
 * Oracle constraint manager
 */
public class OracleConstraintManager extends JDBCConstraintManager<OracleConstraint, OracleTableBase> {

    protected OracleConstraint createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor, DBECommandContext context, OracleTableBase parent,
        Object from)
    {
        EditConstraintDialog editDialog = new EditConstraintDialog(
            workbenchWindow.getShell(),
            OracleMessages.edit_oracle_constraint_manager_dialog_title,
            parent,
            new DBSConstraintType[] {
                DBSConstraintType.PRIMARY_KEY,
                DBSConstraintType.UNIQUE_KEY });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final OracleConstraint constraint = new OracleConstraint(
            parent,
            null,
            editDialog.getConstraintType(),
            null,
            OracleObjectStatus.ENABLED);
        constraint.setName(DBObjectNameCaseTransformer.transformName(constraint, CommonUtils.escapeIdentifier(parent.getName()) + "_PK")); //$NON-NLS-1$
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            constraint.addColumn(
                new OracleConstraintColumn(
                    constraint,
                    (OracleTableColumn) tableColumn,
                    colIndex++));
        }
        return constraint;
    }

    protected String getDropConstraintPattern(OracleConstraint constraint)
    {
        String clause;
        if (constraint.getConstraintType() == DBSConstraintType.PRIMARY_KEY) {
            clause = "PRIMARY KEY"; //$NON-NLS-1$
        } else {
            clause = "KEY"; //$NON-NLS-1$
        }
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
