/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
import org.jkiss.utils.CommonUtils;

/**
 * Postgre constraint manager
 */
public class PostgreConstraintManager extends SQLConstraintManager<PostgreTableConstraintBase, PostgreTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreTableConstraintBase> getObjectsCache(PostgreTableConstraintBase object)
    {
        return object.getTable().getContainer().constraintCache;
    }

    @Override
    protected PostgreTableConstraintBase createDatabaseObject(
        DBECommandContext context, PostgreTableBase parent,
        Object from)
    {
        EditConstraintDialog editDialog = new EditConstraintDialog(
            DBeaverUI.getActiveWorkbenchShell(),
            "Add constraint",
            parent,
            new DBSEntityConstraintType[] {
                DBSEntityConstraintType.PRIMARY_KEY,
                DBSEntityConstraintType.UNIQUE_KEY });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final PostgreTableConstraint constraint = new PostgreTableConstraint(
            parent,
            editDialog.getConstraintType());
        constraint.setName(DBObjectNameCaseTransformer.transformObjectName(constraint, CommonUtils.escapeIdentifier(parent.getName()) + "_PK")); //$NON-NLS-1$
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            constraint.addColumn(
                new PostgreTableConstraintColumn(
                    constraint,
                    (PostgreAttribute) tableColumn,
                    colIndex++));
        }
        return constraint;
    }

    @NotNull
    protected String getAddConstraintTypeClause(PostgreTableConstraintBase constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE"; //$NON-NLS-1$
        }
        return super.getAddConstraintTypeClause(constraint);
    }

    @Override
    protected String getDropConstraintPattern(PostgreTableConstraintBase constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
