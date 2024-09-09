/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.gaussdb.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBProcedure;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBSchema;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * GaussDBProcedureManager
 */
public class GaussDBProcedureManager extends SQLObjectEditor<GaussDBProcedure, GaussDBSchema>
    implements DBEObjectRenamer<GaussDBProcedure> {

    @Nullable
    @Override
    public DBSObjectCache<GaussDBSchema, GaussDBProcedure> getObjectsCache(GaussDBProcedure object) {
        GaussDBSchema schema = (GaussDBSchema) object.getContainer();
        return schema.getGaussDBProceduresCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return container instanceof PostgreSchema
            && ((PostgreSchema) container).getDataSource().getServerType().supportsFunctionCreate();
    }

    @Override
    public boolean canDeleteObject(@NotNull GaussDBProcedure object) {
        return object.getDataSource().getServerType().supportsFunctionCreate();
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command,
        Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Function name cannot be empty");
        }
    }

    @Override
    protected GaussDBProcedure createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container,
                                                    Object copyFrom, @NotNull Map<String, Object> options) {
        return new GaussDBProcedure((PostgreSchema) container);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceProcedureQuery(actionList, command.getObject());
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        String objectType = command.getObject().getProcedureTypeName();
        actions.add(
            new SQLDatabasePersistAction("Drop function", "DROP " + objectType + " " + command.getObject().getFullQualifiedSignature()) //$NON-NLS-2$
        );
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, GaussDBProcedure procedure) {
        actions.add(new SQLDatabasePersistAction("Create function", procedure.getBody(), true));
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                         @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<GaussDBProcedure, PropertyHandler> command, @NotNull Map<String, Object> options) {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actions.add(new SQLDatabasePersistAction("Comment function",
                "COMMENT ON " + command.getObject().getProcedureTypeName() + " " + command.getObject().getFullQualifiedSignature()
                    + " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
        boolean isDDL = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SOURCE);
        if (isDDL) {
            try {
                PostgreUtils.getObjectGrantPermissionActions(monitor, command.getObject(), actions, options);
            } catch (DBException e) {
                log.error(e);
            }
        }

    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull GaussDBProcedure object,
        @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        GaussDBProcedure procedure = command.getObject();
        actions.add(new SQLDatabasePersistAction("Rename function",
            "ALTER " + command.getObject().getProcedureTypeName() + " " + DBUtils.getQuotedIdentifier(procedure.getSchema()) + "."
                + GaussDBProcedure.makeOverloadedName(procedure.getSchema(), command.getOldName(), procedure.getParameters(monitor),
                    true, false, false)
                + " RENAME TO " + DBUtils.getQuotedIdentifier(procedure.getDataSource(), command.getNewName())));
    }
}
