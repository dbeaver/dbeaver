/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public DBSObjectCache<MySQLDatabase, MySQLTableConstraint> getObjectsCache(MySQLTableConstraint object)
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
