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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObjectClass;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class SQLServerDatabaseManager extends SQLObjectEditor<SQLServerDatabase, SQLServerDataSource> implements DBEObjectRenamer<SQLServerDatabase> {

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerDatabase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected SQLServerDatabase createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        return new SQLServerDatabase((SQLServerDataSource) container);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction(
                "Create database",
                "CREATE DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + ";"
        ));
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        if (CommonUtils.getOption(options, DBEObjectManager.OPTION_CLOSE_EXISTING_CONNECTIONS)) {
            actions.add(new SQLDatabasePersistAction(
                "Drop database connections",
                "ALTER DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE;"
            ));
        }
        actions.add(new SQLDatabasePersistAction(
                "Drop database",
                "DROP DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + ";"
        ));
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        final SQLServerDataSource source = command.getObject().getDataSource();
        final String oldName = DBUtils.getQuotedIdentifier(source, command.getOldName());
        final String newName = DBUtils.getQuotedIdentifier(source, command.getNewName());

        actions.add(new SQLDatabasePersistAction(
                "Rename database",
                "ALTER DATABASE " + oldName + " MODIFY NAME = " + newName + ";"
        ));
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<SQLServerDatabase, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        // The property is present with a null value when the description is cleared, so the key
        // has to be tested rather than the value - otherwise clearing a description does nothing
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            return;
        }
        final SQLServerDatabase database = command.getObject();
        final String description = CommonUtils.toString(command.getProperty(DBConstants.PROP_ID_DESCRIPTION), null);
        final boolean commentSet = SQLServerUtils.isCommentSet(
            monitor,
            database,
            SQLServerObjectClass.DATABASE,
            0,
            0);
        if (CommonUtils.isEmpty(description) && !commentSet) {
            return;
        }
        final StringBuilder sql = new StringBuilder("EXEC ")
            .append(SQLServerUtils.getSystemTableName(
                database,
                CommonUtils.isEmpty(description)
                    ? "sp_dropextendedproperty"
                    : commentSet ? "sp_updateextendedproperty" : "sp_addextendedproperty"))
            .append(" '").append(SQLServerConstants.PROP_MS_DESCRIPTION).append("'");
        if (!CommonUtils.isEmpty(description)) {
            sql.append(", ").append(SQLUtils.quoteString(database, description));
        }
        actions.add(new SQLDatabasePersistAction("Set database comment", sql.toString()));
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY | DBEObjectMaker.FEATURE_CLOSE_EXISTING_CONNECTIONS;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerDatabase> getObjectsCache(SQLServerDatabase object) {
        return object.getDataSource().getDatabaseCache();
    }
}
