/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class SQLServerDatabaseManager extends SQLObjectEditor<SQLServerDatabase, SQLServerDataSource> implements DBEObjectRenamer<SQLServerDatabase> {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerDatabase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected SQLServerDatabase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return new SQLServerDatabase((SQLServerDataSource) container);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction(
                "Create database",
                "CREATE DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + ";"
        ));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
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
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        final SQLServerDataSource source = command.getObject().getDataSource();
        final String oldName = DBUtils.getQuotedIdentifier(source, command.getOldName());
        final String newName = DBUtils.getQuotedIdentifier(source, command.getNewName());

        actions.add(new SQLDatabasePersistAction(
                "Rename database",
                "ALTER DATABASE " + oldName + " MODIFY NAME = " + newName + ";"
        ));
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY | DBEObjectMaker.FEATURE_CLOSE_EXISTING_CONNECTIONS;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerDatabase> getObjectsCache(SQLServerDatabase object) {
        return object.getDataSource().getDatabaseCache();
    }
}
