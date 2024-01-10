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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * SQLServerTableTrigger
 */
public class SQLServerDatabaseTrigger extends SQLServerTriggerBase<SQLServerDatabase> {

    private SQLServerDatabase database;

    SQLServerDatabaseTrigger(
        SQLServerDatabase database,
        ResultSet dbResult)
    {
        super(database, dbResult);
        this.database = database;
    }

    public SQLServerDatabaseTrigger(
        SQLServerDatabase database,
        String name)
    {
        super(database, name);
        this.database = database;
    }

    public SQLServerDatabaseTrigger(SQLServerDatabase database, SQLServerDatabaseTrigger source) {
        super(database, source);
        this.database = database;
    }

    @Override
    public SQLServerTable getTable()
    {
        return null;
    }

    @NotNull
    public SQLServerDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getDatabase(),
            this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getTriggerCache().refreshObject(monitor, getDatabase(), this);
    }

}
