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
package org.jkiss.dbeaver.ext.hana.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.hana.model.HANADataSource;
import org.jkiss.dbeaver.ext.hana.model.HANASchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class HANASchemaManager extends SQLObjectEditor<HANASchema, HANADataSource> implements DBEObjectRenamer<HANASchema> {

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(@NotNull HANASchema object) {
        return true;
    }

    @Override
    protected String getBaseObjectName() {
        return "NEW_SCHEMA";
    }

    @Override
    protected HANASchema createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        if (container instanceof HANADataSource) {
            HANADataSource dataSource = (HANADataSource) container;
            HANASchema schema = new HANASchema(dataSource, null, "NEW_SCHEMA");
            setNewObjectName(monitor, dataSource, schema);
            return schema;
        }
        return null;
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(
            new SQLDatabasePersistAction(
                "Create schema", //$NON-NLS-1$
                "CREATE SCHEMA " + DBUtils.getObjectFullName(command.getObject(), DBPEvaluationContext.DDL)) //$NON-NLS-1$
        );
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull HANASchema object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        HANASchema schema = command.getObject();
        GenericDataSource dataSource = schema.getDataSource();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename schema",
                "RENAME SCHEMA " + //$NON-NLS-1$
                    DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) +
                    " TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        // RESTRICT drops the schema, but only when there are no objects in it.
        // If RESTRICT is specified while there are still objects in the schema, then an error is returned.
        // But we will not use CASCADE here - too dangerous.
        actions.add(
            new SQLDatabasePersistAction(
                "Drop schema", //$NON-NLS-1$
                "DROP SCHEMA " + DBUtils.getObjectFullName(command.getObject(), DBPEvaluationContext.DDL) + " RESTRICT") //$NON-NLS-1$
        );
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, HANASchema> getObjectsCache(HANASchema object) {
        return object.getDataSource().getSchemaCache();
    }
}
