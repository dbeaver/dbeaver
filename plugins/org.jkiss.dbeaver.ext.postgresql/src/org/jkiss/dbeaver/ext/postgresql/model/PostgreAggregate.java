/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreAggregate
 */
public class PostgreAggregate implements PostgreObject {

    private long oid;
    private PostgreSchema schema;
    private String name;
    private boolean persisted;

    public PostgreAggregate(PostgreSchema schema, ResultSet dbResult)
        throws SQLException
    {
        this.schema = schema;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "proc_oid");
        this.name = JDBCUtils.safeGetString(dbResult, "proc_name");

        this.persisted = true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public List<PostgreDataType> getInputTypes(DBRProgressMonitor monitor) throws DBException {
        PostgreProcedure function = getFunction(monitor);
        if (function == null) {
            return null;
        }
        List<PostgreDataType> result = new ArrayList<>();
        for (PostgreProcedureParameter param : function.getInputParameters()) {
            result.add(param.getParameterType());
        }
        return result;
    }

    @Property(viewable = true, order = 3)
    public PostgreDataType getOutputType(DBRProgressMonitor monitor) throws DBException {
        PostgreProcedure function = getFunction(monitor);
        return function == null ? null : function.getReturnType();
    }

    @Property(viewable = false, order = 80)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 10)
    public PostgreProcedure getFunction(DBRProgressMonitor monitor) throws DBException {
        return schema.getProcedure(monitor, this.oid);
    }

    @Override
    public DBSObject getParentObject() {
        return schema;
    }

    @Override
    public PostgreDataSource getDataSource() {
        return schema.getDataSource();
    }

    @Override
    public PostgreDatabase getDatabase() {
        return schema.getDatabase();
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }
}

