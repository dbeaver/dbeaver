/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * DB2 Schema Manager
 * 
 * @author Denis Forveille
 */
public class DB2SchemaManager extends SQLObjectEditor<DB2Schema, DB2DataSource> {

    private static final String SQL_CREATE_SCHEMA = "CREATE SCHEMA %s";
    private static final String SQL_DROP_SCHEMA = "DROP SCHEMA %s RESTRICT";

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2Schema> getObjectsCache(DB2Schema object)
    {
        return object.getDataSource().getSchemaCache();
    }

    @Override
    protected DB2Schema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
                                             Object copyFrom, Map<String, Object> options)
    {
        return new DB2Schema((DB2DataSource) container, "NEW_SCHEMA");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        SQLDatabasePersistAction action = new SQLDatabasePersistAction("Create schema", String.format(SQL_CREATE_SCHEMA,
            DBUtils.getQuotedIdentifier(command.getObject())));
        actions.add(action);
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        String schemaName = command.getObject().getName();
        DBEPersistAction action = new SQLDatabasePersistAction("Drop schema (SQL)", String.format(SQL_DROP_SCHEMA,
            DBUtils.getQuotedIdentifier(command.getObject())));
        actions.add(action);
    }

}
