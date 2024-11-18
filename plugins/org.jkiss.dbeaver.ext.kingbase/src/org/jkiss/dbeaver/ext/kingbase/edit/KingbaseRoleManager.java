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
package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRole;
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

/**
 * KingbaseRoleManager
 */
public class KingbaseRoleManager extends SQLObjectEditor<KingbaseRole, KingbaseDataSource> implements DBEObjectRenamer<KingbaseRole> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, KingbaseRole> getObjectsCache(KingbaseRole object)
    {
        return null;
    }

    @Override
    protected KingbaseRole createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        return new KingbaseRole((KingbaseDatabase) container, "NewRole", "", true);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        final KingbaseRole role = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE ROLE " + DBUtils.getQuotedIdentifier(role));
        addRoleOptions(script, role, command, true);

        actions.add(
            new SQLDatabasePersistAction("Create role", script.toString()) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            final KingbaseRole role = command.getObject();
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
        final KingbaseRole role = command.getObject();
        final DBPDataSource dataSource = role.getDataSource();

        actions.add(new SQLDatabasePersistAction(
            "Rename role",
            "ALTER ROLE " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseRole object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    private void addRoleOptions(StringBuilder script, KingbaseRole role, NestedObjectCommand command, boolean create) {
        final StringBuilder options = new StringBuilder();
        if (role.isSuperUser()) options.append(" SUPERUSER"); else options.append(" NOSUPERUSER");
        if (role.isCreateDatabase()) options.append(" CREATEDB"); else options.append(" NOCREATEDB");
        
        if (role.isCreateRole()) options.append(" CREATEROLE"); else options.append(" NOCREATEROLE");
        
        if (role.isCanLogin()) options.append(" LOGIN"); else options.append(" NOLOGIN");

        
        if (role.isReplication()) {
            options.append(" REPLICATION");
        } else {
            options.append(" NOREPLICATION");
        }
        

        if (create && !CommonUtils.isEmpty(role.getPassword())) {
            // A password is only of use for roles having the LOGIN attribute, but you can nonetheless define one for roles without it
            options.append(" PASSWORD ").append("'").append(role.getDataSource().getSQLDialect().escapeString(role.getPassword())).append("'");
            command.setDisableSessionLogging(true); // Hide password from Query Manager
        }

        script.append(options);
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<KingbaseRole, PropertyHandler> command,
        @NotNull Map<String, Object> options)
    {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            KingbaseRole role = command.getObject();
            actions.add(new SQLDatabasePersistAction(
                "Comment role",
                "COMMENT ON ROLE " + DBUtils.getQuotedIdentifier(role.getDataSource(), role.getName()) +
                    " IS " + SQLUtils.quoteString(role, CommonUtils.notEmpty(role.getDescription()))));
        }
    }
}
