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
package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.ext.cubrid.model.CubridServer;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
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

public class CubridServerManager extends SQLObjectEditor<CubridServer, GenericStructContainer> implements DBEObjectRenamer<CubridServer> {

    public static final String BASE_SERVER_NAME = "new_server";

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, CubridServer> getObjectsCache(CubridServer object) {
        DBSObject parentObject = object.getParentObject();
        if (parentObject instanceof CubridDataSource container) {
            return container.getServerCache();
        }
        return null;
    }

    @Override
    protected CubridServer createDatabaseObject(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBECommandContext context,
            @Nullable Object container,
            @Nullable Object copyFrom,
            @NotNull Map<String, Object> options) {
        return new CubridServer((CubridDataSource) container, BASE_SERVER_NAME);
    }

    @Override
    protected void addObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectCreateCommand command,
            @NotNull Map<String, Object> options) {
        CubridServer server = command.getObject();
        StringBuilder query = new StringBuilder();
        query.append("CREATE SERVER ");
        query.append(server.getOwner() + "." + server.getName());
        query.append(" (HOST=").append(SQLUtils.quoteString(server, server.getHost()));
        if (server.getPort() != null) {
            query.append(", PORT=").append(server.getPort());
        }
        if (server.getDbName() != null) {
            query.append(", DBNAME=").append(server.getDbName());
        }
        additionalCreateActions(server, query);
        query.append(")");
        actions.add(new SQLDatabasePersistAction("Create Server", query.toString()));
    }

    public void additionalCreateActions(@NotNull CubridServer server, @NotNull StringBuilder query) {
        if (server.getUserName() != null) {
            query.append(", USER=").append(server.getUserName());
        }
        if (server.getPassword() != null) {
            query.append(", PASSWORD=").append(SQLUtils.quoteString(server, server.getPassword()));
        }
        if (server.getProperties() != null) {
            query.append(", PROPERTIES=").append(SQLUtils.quoteString(server, server.getProperties()));
        }
        if (server.getDescription() != null) {
            query.append(", COMMENT=").append(SQLUtils.quoteString(server, server.getDescription()));
        }
    }

    @Override
    protected void addObjectModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectChangeCommand command,
            @NotNull Map<String, Object> options) {
        CubridServer server = command.getObject();
        String suffix = ",";
        StringBuilder query = new StringBuilder();
        query.append("ALTER SERVER ");
        query.append(server.getOwner() + "." + server.getName());
        if (command.getProperty("host") != null && server.getHost() != null) {
            query.append(" CHANGE HOST=").append(SQLUtils.quoteString(server, server.getHost())).append(suffix);
        }
        if (command.getProperty("port") != null && server.getPort() != null) {
            query.append(" CHANGE PORT=").append(server.getPort()).append(suffix);
        }
        if (command.getProperty("dbName") != null && server.getDbName() != null) {
            query.append(" CHANGE DBNAME=").append(server.getDbName()).append(suffix);
        }
        additionalModifyActions(server, query, command, suffix);
        query.deleteCharAt(query.length() - 1);
        actions.add(new SQLDatabasePersistAction("Alter Server", query.toString()));
    }

    public void additionalModifyActions(
            @NotNull CubridServer server,
            @NotNull StringBuilder query,
            @NotNull ObjectChangeCommand command,
            @NotNull String suffix) {
        if (command.getProperty("userName") != null && server.getUserName() != null) {
            query.append(" CHANGE USER=").append(server.getUserName()).append(suffix);
        }
        if (command.getProperty("password") != null && server.getPassword() != null) {
            query.append(" CHANGE PASSWORD=").append(SQLUtils.quoteString(server, server.getPassword())).append(suffix);
        }
        if (command.getProperty("properties") != null && server.getProperties() != null) {
            query.append(" CHANGE PROPERTIES=").append(SQLUtils.quoteString(server, server.getProperties())).append(suffix);
        }
        if (command.getProperty("description") != null && server.getDescription() != null) {
            query.append(" CHANGE COMMENT=").append(SQLUtils.quoteString(server, server.getDescription())).append(suffix);
        }
    }

    @Override
    protected void addObjectDeleteActions(
            @NotNull DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectDeleteCommand command,
            @NotNull Map<String, Object> options)
            throws DBException {
        CubridServer server = command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop Server",
        "DROP SERVER " + server.getOwner() + "." + server.getName()));
    }

    @Override
    protected void addObjectRenameActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull ObjectRenameCommand command,
            @NotNull Map<String, Object> options) {
        CubridServer server = command.getObject();
        actions.add(new SQLDatabasePersistAction("Rename Server",
        "RENAME SERVER " + server.getOwner() + "." + command.getOldName() + " TO " + command.getNewName()));
    }

    @Override
    public void renameObject(
            @NotNull DBECommandContext commandContext,
            @NotNull CubridServer object,
            @NotNull Map<String, Object> options,
            @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

}
