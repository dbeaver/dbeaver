/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.postgresql.internal.PostgreSQLMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgrePublication;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class PostgrePublicationManager extends SQLObjectEditor<PostgrePublication, PostgreDatabase> {
    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @NotNull
    @Override
    public DBSObjectCache<PostgreDatabase, PostgrePublication> getObjectsCache(@NotNull PostgrePublication object) {
        return object.getDatabase().getPublicationCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return container instanceof PostgreDatabase database && database.getDataSource().getServerType().supportsLogicalReplication()
            && super.canCreateObject(container);
    }

    @Override
    public boolean canEditObject(@NotNull PostgrePublication object) {
        return !object.isPersisted() && super.canEditObject(object);
    }

    @NotNull
    @Override
    protected PostgrePublication createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, @NotNull Object container,
        @Nullable Object copyFrom, @NotNull Map<String, Object> options
    ) {
        return new PostgrePublication((PostgreDatabase) container, "new_publication");
    }

    @Override
    protected void validateObjectProperties(
        @NotNull DBRProgressMonitor monitor, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options
    ) throws DBException {
        PostgrePublication publication = command.getObject();
        if (publication.getName().isBlank()) {
            throw new DBException("Publication name cannot be empty");
        }
        if (publication.isAllTables() && !publication.getTables(monitor).isEmpty()) {
            throw new DBException("FOR ALL TABLES cannot be combined with a table list");
        }
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options
    ) throws DBException {
        actions.add(new SQLDatabasePersistAction(
            PostgreSQLMessages.action_create_publication, command.getObject().getObjectDefinitionText(monitor, options)
        ));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction(
            PostgreSQLMessages.action_drop_publication, "DROP PUBLICATION " + DBUtils.getQuotedIdentifier(command.getObject())
        ));
    }
}
