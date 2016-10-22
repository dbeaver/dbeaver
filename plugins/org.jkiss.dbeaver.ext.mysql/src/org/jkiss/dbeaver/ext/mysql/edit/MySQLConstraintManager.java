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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

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
        DBRProgressMonitor monitor, DBECommandContext context, final MySQLTable parent,
        Object from)
    {
        return new UITask<MySQLTableConstraint>() {
            @Override
            protected MySQLTableConstraint runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    MySQLMessages.edit_constraint_manager_title,
                    parent,
                    new DBSEntityConstraintType[] {
                        DBSEntityConstraintType.PRIMARY_KEY,
                        DBSEntityConstraintType.UNIQUE_KEY });
                if (!editPage.edit()) {
                    return null;
                }

                final MySQLTableConstraint constraint = new MySQLTableConstraint(
                    parent,
                    editPage.getConstraintName(),
                    null,
                    editPage.getConstraintType(),
                    false);
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    constraint.addColumn(
                        new MySQLTableConstraintColumn(
                            constraint,
                            (MySQLTableColumn) tableColumn,
                            colIndex++));
                }
                return constraint;
            }
        }.execute();
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
