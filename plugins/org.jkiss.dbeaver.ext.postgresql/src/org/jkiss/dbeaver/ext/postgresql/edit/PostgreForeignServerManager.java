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

package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreForeignServer;
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

public class PostgreForeignServerManager extends SQLObjectEditor<PostgreForeignServer, PostgreDatabase> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<PostgreDatabase, PostgreForeignServer> getObjectsCache(PostgreForeignServer object) {
        return object.getDatabase().foreignServerCache;
    }

    @Override
    protected PostgreForeignServer createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context,
        Object container, Object copyFrom, Map<String, Object> options) throws DBException
    {
        return new PostgreForeignServer((PostgreDatabase) container);
    }


    @Override
    protected void addObjectCreateActions(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext, List<DBEPersistAction> actions,
        ObjectCreateCommand command,
        Map<String, Object> options) {
        final PostgreForeignServer foreignServer = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Create extension",
                "CREATE FOREIGN SERVER " + foreignServer.getName()));
    }

    @Override
    protected void addObjectDeleteActions(
        DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
        ObjectDeleteCommand command,
        Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Drop extension", "DROP FOREIGN SERVER " + DBUtils.getQuotedIdentifier(command.getObject())));
    }

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(PostgreForeignServer object) {
        return true;
    }

    @Override
    public boolean canEditObject(PostgreForeignServer object) {
        return false;
    }

}
