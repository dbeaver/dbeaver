/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * OracleMaterializedViewManager
 */
public class OracleMaterializedViewManager extends SQLObjectEditor<OracleMaterializedView, OracleSchema> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty"); //$NON-NLS-1$
        }
        if (CommonUtils.isEmpty(command.getObject().getObjectDefinitionText(null))) {
            throw new DBException("View definition cannot be empty"); //$NON-NLS-1$
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleMaterializedView> getObjectsCache(OracleMaterializedView object)
    {
        return object.getSchema().mviewCache;
    }

    @Override
    protected OracleMaterializedView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, OracleSchema parent, Object copyFrom)
    {
        OracleMaterializedView newView = new OracleMaterializedView(parent, "NewView"); //$NON-NLS-1$
        return newView;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command)
    {
        createOrReplaceViewQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command)
    {
        createOrReplaceViewQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(List<DBEPersistAction> actions, OracleMaterializedView view)
    {
        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(lineSeparator) //$NON-NLS-1$
            .append("AS ").append(view.getObjectDefinitionText(null)); //$NON-NLS-1$
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$
        actions.add(
            new SQLDatabasePersistAction("Create view", decl.toString()));
    }

}

