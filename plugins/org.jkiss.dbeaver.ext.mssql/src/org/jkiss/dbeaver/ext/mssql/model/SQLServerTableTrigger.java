/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * SQLServerTableTrigger
 */
public class SQLServerTableTrigger extends SQLServerTriggerBase<SQLServerTable>
{

    public SQLServerTableTrigger(
        SQLServerTable table,
        ResultSet dbResult)
    {
        super(table, dbResult);
    }

    public SQLServerTableTrigger(
        SQLServerTable table,
        String name)
    {
        super(table, name);
    }

    public SQLServerTableTrigger(SQLServerTable table, SQLServerTableTrigger source) {
        super(table, source);
    }

    @Override
    @Property(viewable = true, order = 4)
    public SQLServerTable getTable()
    {
        return getParentObject();
    }

    public SQLServerSchema getSchema() {
        return getParentObject().getSchema();
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            this);
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        return getTable().getSchema().getTriggerCache().refreshObject(monitor, getSchema(), this);
    }

}
