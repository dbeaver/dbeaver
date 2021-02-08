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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * PostgreForeignDataWrapper
 */
public class PostgreForeignDataWrapper extends PostgreInformation implements PostgreScriptObject {

    private long oid;
    private String name;
    private String[] options;
    private long ownerId;
    private long handlerProcId;
    private long validatorProcId;
    private long handlerSchemaId;

    public PostgreForeignDataWrapper(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        super(database);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "fdwname");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "fdwowner");
        this.handlerProcId = JDBCUtils.safeGetLong(dbResult, "fdwhandler");
        this.validatorProcId = JDBCUtils.safeGetLong(dbResult, "fdwvalidator");
        this.handlerSchemaId = JDBCUtils.safeGetLong(dbResult, "handler_schema_id");
        this.options = JDBCUtils.safeGetArray(dbResult, "fdwoptions");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 5)
    public String[] getOptions() {
        return options;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = false, order = 8)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(viewable = false, order = 10)
    public PostgreProcedure getHandler(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getProcedure(monitor, handlerSchemaId, handlerProcId);
    }

    @Property(viewable = false, order = 10)
    public PostgreProcedure getValidator(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getProcedure(monitor, handlerSchemaId, validatorProcId);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        PostgreProcedure handler = getHandler(monitor);
        PostgreProcedure validator = getValidator(monitor);
        return
            "-- Foreign data wrapper: " + getName() + "\n\n" +
                "-- DROP FOREIGN DATA WRAPPER " + getName() + ";\n\n" +
                "CREATE FOREIGN DATA WRAPPER " + getName() + "\n\t" +
                (handler == null ? "" : "HANDLER " + handler.getName() + "\n\t") +
                (validator == null ? "" : "VALIDATOR " + validator.getName() + "\n\t") +
                "OPTIONS " + PostgreUtils.getOptionsString(this.options);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }


}
