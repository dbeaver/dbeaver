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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * PostgreSchemaManager
 */
public class PostgreSchemaManager extends SQLObjectEditor<PostgreSchema, PostgreDatabase> implements DBEObjectRenamer<PostgreSchema> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<PostgreDatabase, PostgreSchema> getObjectsCache(PostgreSchema object)
    {
        return object.getDatabase().schemaCache;
    }

    @Override
    protected PostgreSchema createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        PostgreDatabase database = (PostgreDatabase) container;
        return database.createSchemaImpl(database, "NewSchema", (PostgreRole) null);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options)
    {
        final PostgreSchema schema = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA " + DBUtils.getQuotedIdentifier(schema));
        try {
            final PostgreRole owner = schema.getOwner(monitor);
            if (owner != null) {
                script.append("\nAUTHORIZATION ").append(DBUtils.getQuotedIdentifier(owner));
            }
        } catch (DBException e) {
            log.error(e);
        }

        actions.add(
            new CreateSchemaAction(schema, script) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename schema",
                "ALTER SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreSchema schema, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException
    {
        processObjectRename(commandContext, schema, options, newName);
    }

    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<PostgreSchema, PropertyHandler> command, @NotNull Map<String, Object> options)
    {
        PostgreSchema schema = command.getObject();
        String comment = schema.getDescription();

        if (comment != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment schema",
                "COMMENT ON SCHEMA " + DBUtils.getQuotedIdentifier(schema) +
                    " IS " + SQLUtils.quoteString(schema, comment)));
        }
    }

    private static class CreateSchemaAction extends SQLDatabasePersistActionAtomic {
        private final PostgreSchema schema;

        public CreateSchemaAction(PostgreSchema schema, StringBuilder sql) {
            super("Create schema", sql.toString());
            this.schema = schema;
        }

        @Override
        public void afterExecute(DBCSession session, Throwable error) throws DBCException {
            super.afterExecute(session, error);
            if (error == null) {
                schema.readSchemaInfo(session.getProgressMonitor());
            }
        }
    }

}

