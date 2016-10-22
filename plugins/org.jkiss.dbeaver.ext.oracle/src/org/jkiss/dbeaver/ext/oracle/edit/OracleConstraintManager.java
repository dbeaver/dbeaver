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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * Oracle constraint manager
 */
public class OracleConstraintManager extends SQLConstraintManager<OracleTableConstraint, OracleTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableConstraint> getObjectsCache(OracleTableConstraint object)
    {
        return object.getParentObject().getSchema().constraintCache;
    }

    @Override
    protected OracleTableConstraint createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final OracleTableBase parent,
        Object from)
    {
        return new UITask<OracleTableConstraint>() {
            @Override
            protected OracleTableConstraint runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    OracleMessages.edit_oracle_constraint_manager_dialog_title,
                    parent,
                    new DBSEntityConstraintType[] {
                        DBSEntityConstraintType.PRIMARY_KEY,
                        DBSEntityConstraintType.UNIQUE_KEY });
                if (!editPage.edit()) {
                    return null;
                }

                final OracleTableConstraint constraint = new OracleTableConstraint(
                    parent,
                    editPage.getConstraintName(),
                    editPage.getConstraintType(),
                    null,
                    OracleObjectStatus.ENABLED);
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    constraint.addColumn(
                        new OracleTableConstraintColumn(
                            constraint,
                            (OracleTableColumn) tableColumn,
                            colIndex++));
                }
                return constraint;
            }
        }.execute();
    }

    @Override
    protected String getDropConstraintPattern(OracleTableConstraint constraint)
    {
        String clause;
        if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
            clause = "PRIMARY KEY"; //$NON-NLS-1$
        } else {
            clause = "CONSTRAINT"; //$NON-NLS-1$
        }
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @NotNull
    protected String getAddConstraintTypeClause(OracleTableConstraint constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE"; //$NON-NLS-1$
        }
        return super.getAddConstraintTypeClause(constraint);
    }

}
