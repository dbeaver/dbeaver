/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

/**
 * PostgreForeignDataWrapper
 */
public class PostgreForeignDataWrapper extends PostgreInformation {

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
    public PostgreAuthId getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, getDatabase().authIdCache, getDatabase(), ownerId);
    }

    @Property(viewable = false, order = 10)
    public PostgreProcedure getHandler(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getProcedure(monitor, handlerSchemaId, handlerProcId);
    }

    @Property(viewable = false, order = 10)
    public PostgreProcedure getValidator(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getProcedure(monitor, handlerSchemaId, validatorProcId);
    }

}
