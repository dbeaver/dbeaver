/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * OracleMaterializedViewManager
 */
public class OracleMaterializedViewManager extends SQLObjectEditor<OracleMaterializedView, OracleSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty"); //$NON-NLS-1$
        }
        if (CommonUtils.isEmpty(command.getObject().getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS))) {
            throw new DBException("View definition cannot be empty"); //$NON-NLS-1$
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleMaterializedView> getObjectsCache(OracleMaterializedView object)
    {
        return (DBSObjectCache) object.getSchema().tableCache;
    }

    @Override
    protected OracleMaterializedView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        OracleMaterializedView newView = new OracleMaterializedView((OracleSchema) container, "NewView"); //$NON-NLS-1$
        newView.setObjectDefinitionText("SELECT 1 FROM DUAL");
        return newView;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actions, command);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actionList, command);
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(List<DBEPersistAction> actions, DBECommandComposite<OracleMaterializedView, PropertyHandler> command)
    {
        OracleMaterializedView view = command.getObject();

        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        boolean hasComment = command.getProperty("comment") != null;
        if (!hasComment || command.getProperties().size() > 1) {
            String mViewDefinition = view.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS).trim();
            if (mViewDefinition.startsWith("CREATE MATERIALIZED VIEW")) {
                if (mViewDefinition.endsWith(";")) mViewDefinition = mViewDefinition.substring(0, mViewDefinition.length() - 1);
                decl.append(mViewDefinition);
            } else {
                decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(lineSeparator) //$NON-NLS-1$
                    .append("AS ").append(mViewDefinition); //$NON-NLS-1$
            }
            if (view.isPersisted()) {
                actions.add(
                    new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$
            }
            actions.add(
                new SQLDatabasePersistAction("Create view", decl.toString()));
        }
        if (hasComment) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS '" + view.getComment() + "'"));
        }
    }

}

