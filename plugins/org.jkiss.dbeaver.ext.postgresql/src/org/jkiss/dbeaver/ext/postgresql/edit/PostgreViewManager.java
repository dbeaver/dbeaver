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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreView;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreViewBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * PostgreViewManager
 */
public class PostgreViewManager extends SQLObjectEditor<PostgreTableBase, PostgreSchema> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreTableBase> getObjectsCache(PostgreTableBase object)
    {
        return object.getContainer().tableCache;
    }

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        PostgreTableBase object = command.getObject();
        if (CommonUtils.isEmpty(object.getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(((PostgreViewBase) object).getSource())) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Override
    protected PostgreViewBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, PostgreSchema parent, Object copyFrom)
    {
        PostgreView newCatalog = new PostgreView(parent);
        newCatalog.setName("new_view"); //$NON-NLS-1$
        return newCatalog;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        createOrReplaceViewQuery(actions, (PostgreViewBase) command.getObject());
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        createOrReplaceViewQuery(actionList, (PostgreViewBase) command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        PostgreViewBase view = (PostgreViewBase)command.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP " + view.getViewType() + " " + view.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected void createOrReplaceViewQuery(List<DBEPersistAction> actions, PostgreViewBase view)
    {
        String createSQL = (view instanceof PostgreView ? "CREATE OR REPLACE " : "CREATE ");
        String ddl = createSQL + view.getViewType() + " " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\n" + view.getSource();

        actions.add(
            new SQLDatabasePersistAction("Create view", ddl));
    }

}

