/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
import org.jkiss.utils.CommonUtils;

/**
 * MySQL constraint manager
 */
public class MySQLConstraintManager extends SQLConstraintManager<MySQLTableConstraint, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableConstraint> getObjectsCache(MySQLTableConstraint object)
    {
        return object.getTable().getContainer().getConstraintCache();
    }

    @Override
    protected MySQLTableConstraint createDatabaseObject(
        DBECommandContext context, MySQLTable parent,
        Object from)
    {
        EditConstraintDialog editDialog = new EditConstraintDialog(
            DBeaverUI.getActiveWorkbenchShell(),
            MySQLMessages.edit_constraint_manager_title,
            parent,
            new DBSEntityConstraintType[] {
                DBSEntityConstraintType.PRIMARY_KEY,
                DBSEntityConstraintType.UNIQUE_KEY });
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final MySQLTableConstraint constraint = new MySQLTableConstraint(
            parent,
            null,
            null,
            editDialog.getConstraintType(),
            false);
        constraint.setName(DBObjectNameCaseTransformer.transformObjectName(constraint, CommonUtils.escapeIdentifier(parent.getName()) + "_PK")); //$NON-NLS-1$
        int colIndex = 1;
        for (DBSEntityAttribute tableColumn : editDialog.getSelectedAttributes()) {
            constraint.addColumn(
                new MySQLTableConstraintColumn(
                    constraint,
                    (MySQLTableColumn) tableColumn,
                    colIndex++));
        }
        return constraint;
    }

    @Override
    protected String getDropConstraintPattern(MySQLTableConstraint constraint)
    {
        if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
            return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP PRIMARY KEY"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else {
            return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP KEY " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

}
