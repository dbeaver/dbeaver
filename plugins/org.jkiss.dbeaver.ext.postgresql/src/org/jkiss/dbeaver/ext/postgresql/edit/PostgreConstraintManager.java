/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

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
        DBRProgressMonitor monitor, DBECommandContext context, final PostgreTableBase parent,
        Object from)
    {
        return new UITask<PostgreTableConstraintBase>() {
            @Override
            protected PostgreTableConstraintBase runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    "Add constraint",
                    parent,
                    new DBSEntityConstraintType[] {
                        DBSEntityConstraintType.PRIMARY_KEY,
                        DBSEntityConstraintType.UNIQUE_KEY,
                        DBSEntityConstraintType.CHECK });
                if (!editPage.edit()) {
                    return null;
                }

                final PostgreTableConstraint constraint = new PostgreTableConstraint(
                    parent,
                    editPage.getConstraintName(),
                    editPage.getConstraintType());
                if (constraint.getConstraintType().isCustom()) {
                    constraint.setSource(editPage.getConstraintExpression());
                } else {
                    int colIndex = 1;
                    for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                        constraint.addColumn(
                            new PostgreTableConstraintColumn(
                                constraint,
                                (PostgreAttribute) tableColumn,
                                colIndex++));
                    }
                }
                return constraint;
            }
        }.execute();
    }

    @NotNull
    protected String getAddConstraintTypeClause(PostgreTableConstraintBase constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE"; //$NON-NLS-1$
        }
        return super.getAddConstraintTypeClause(constraint);
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<PostgreTableConstraintBase> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append(" (").append(((PostgreTableConstraint) command.getObject()).getSource()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }

    @Override
    protected String getDropConstraintPattern(PostgreTableConstraintBase constraint)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE +" DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
