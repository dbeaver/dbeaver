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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PostgreViewManager
 */
public class PostgreViewManager extends SQLTableManager<PostgreTableBase, PostgreSchema> {

    private static final Class<?>[] CHILD_TYPES = {
        PostgreTableColumn.class,
    };

    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

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
        PostgreView newView = new PostgreView(parent);
        try {
            newView.setName(getNewChildName(monitor, parent, "new_view"));
        } catch (DBException e) {
            // Never be here
            log.error(e);
        }
        return newView;
    }

    @Override
    protected void addStructObjectCreateActions(List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actions, (PostgreViewBase) command.getObject());
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actionList, (PostgreViewBase) command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        PostgreViewBase view = (PostgreViewBase)command.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP " + view.getViewType() + " " + view.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected void createOrReplaceViewQuery(List<DBEPersistAction> actions, PostgreViewBase view)
    {
        String sql = view.getSource().trim();
        if (!sql.toLowerCase(Locale.ENGLISH).startsWith("create")) {
            sql = "CREATE OR REPLACE VIEW " + DBUtils.getObjectFullName(view, DBPEvaluationContext.DDL) + " AS\n" + sql;
        }
        actions.add(
            new SQLDatabasePersistAction("Create view", sql));
    }

}

