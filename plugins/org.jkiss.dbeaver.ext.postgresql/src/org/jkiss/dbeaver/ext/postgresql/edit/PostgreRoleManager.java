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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.impls.cockroach.PostgreServerCockroachDB;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PostgreRoleManager
 */
public class PostgreRoleManager extends SQLObjectEditor<PostgreRole, PostgreDataSource> implements DBEObjectRenamer<PostgreRole> {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreRole> getObjectsCache(PostgreRole object)
    {
        return null;
    }

    @Override
    protected PostgreRole createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        return new PostgreRole((PostgreDatabase) container, "NewRole", "", true);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        final PostgreRole role = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE ROLE " + DBUtils.getQuotedIdentifier(role));
        addRoleOptions(script, role, command, true);

        actions.add(
            new SQLDatabasePersistAction("Create role", script.toString()) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            final PostgreRole role = command.getObject();
            final StringBuilder script = new StringBuilder("ALTER ROLE " + DBUtils.getQuotedIdentifier(role));
            addRoleOptions(script, role, command, false);

            actionList.add(
                new SQLDatabasePersistAction("Alter role", script.toString()) //$NON-NLS-2$
            );
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        actions.add(
            new SQLDatabasePersistAction("Drop role", "DROP ROLE " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        final PostgreRole role = command.getObject();
        final DBPDataSource dataSource = role.getDataSource();

        actions.add(new SQLDatabasePersistAction(
            "Rename role",
            "ALTER ROLE " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreRole object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    private void addRoleOptions(StringBuilder script, PostgreRole role, NestedObjectCommand command, boolean create) {
        final PostgreServerExtension extension = role.getDataSource().getServerType();
        final StringBuilder options = new StringBuilder();
        if (extension.supportsSuperusers()) {
            if (role.isSuperUser()) options.append(" SUPERUSER"); else options.append(" NOSUPERUSER");
        }
        if (extension.supportsRolesWithCreateDBAbility()) {
            if (role.isCreateDatabase()) options.append(" CREATEDB"); else options.append(" NOCREATEDB");
        }
        if (role.isCreateRole()) options.append(" CREATEROLE"); else options.append(" NOCREATEROLE");
        if (extension.supportsInheritance()) {
            if (role.isInherit()) options.append(" INHERIT"); else options.append(" NOINHERIT");
        }
        if (role.isCanLogin()) options.append(" LOGIN"); else options.append(" NOLOGIN");

        if (extension.supportsRoleReplication()) {
            if (role.isReplication()) {
                options.append(" REPLICATION");
            } else {
                options.append(" NOREPLICATION");
            }
        }
        if (extension.supportsRoleBypassRLS()) {
            if (role.isBypassRls()) {
                options.append(" BYPASSRLS");
            } else {
                options.append(" NOBYPASSRLS");
            }
        }

        if (create && !CommonUtils.isEmpty(role.getPassword())) {
            // A password is only of use for roles having the LOGIN attribute, but you can nonetheless define one for roles without it
            options.append(" PASSWORD ").append("'").append(role.getDataSource().getSQLDialect().escapeString(role.getPassword())).append("'");
            command.setDisableSessionLogging(true); // Hide password from Query Manager
        }

        if (role.getValidUntil() != null) {
            options.append(" VALID UNTIL ").append(SQLUtils.quoteString(role, TIMESTAMP_FORMATTER.format(role.getValidUntil())));
        }

        if (options.length() != 0 && extension instanceof PostgreServerCockroachDB) {
            // FIXME: use some generic approach
            script.append(" WITH");
        }
        script.append(options);
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<PostgreRole, PropertyHandler> command,
        @NotNull Map<String, Object> options)
    {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            PostgreRole role = command.getObject();
            actions.add(new SQLDatabasePersistAction(
                "Comment role",
                "COMMENT ON ROLE " + DBUtils.getQuotedIdentifier(role.getDataSource(), role.getName()) +
                    " IS " + SQLUtils.quoteString(role, CommonUtils.notEmpty(role.getDescription()))));
        }
    }
}
