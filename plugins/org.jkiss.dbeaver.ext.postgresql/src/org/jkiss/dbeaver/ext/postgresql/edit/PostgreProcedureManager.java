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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * PostgreProcedureManager
 */
public class PostgreProcedureManager extends SQLObjectEditor<PostgreProcedure, PostgreSchema> {

    @Nullable
    @Override
    public DBSObjectCache<PostgreSchema, PostgreProcedure> getObjectsCache(PostgreProcedure object)
    {
        return object.getContainer().proceduresCache;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
    }

    @Override
    protected PostgreProcedure createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final PostgreSchema parent, Object copyFrom)
    {
        return new UITask<PostgreProcedure>() {
            @Override
            protected PostgreProcedure runTask() {
                CreateProcedurePage editPage = new CreateProcedurePage(parent);
                if (!editPage.edit()) {
                    return null;
                }
                PostgreProcedure newProcedure = new PostgreProcedure(parent);
                newProcedure.setName(editPage.getProcedureName());
                return newProcedure;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty("description") == null) {
            createOrReplaceProcedureQuery(actionList, command.getObject());
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        String objectType = command.getObject().getProcedureTypeName();
        actions.add(
            new SQLDatabasePersistAction("Drop procedure", "DROP " + objectType + " " + command.getObject().getFullQualifiedSignature()) //$NON-NLS-2$
        );
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, PostgreProcedure procedure)
    {
        actions.add(
            new SQLDatabasePersistAction("Create procedure", procedure.getBody(), true));
    }

    @Override
    protected void addObjectExtraActions(List<DBEPersistAction> actions, NestedObjectCommand<PostgreProcedure, PropertyHandler> command, Map<String, Object> options) {
        if (command.getProperty("description") != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment procedure",
                "COMMENT ON " + command.getObject().getProcedureTypeName() + " " + command.getObject().getFullQualifiedSignature() +
                    " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
    }

}

