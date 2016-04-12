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
 * PostgreForeignServer
 */
public class PostgreForeignServer extends PostgreInformation {

    private long oid;
    private String name;
    private String type;
    private String version;
    private String[] options;
    private long ownerId;
    private long dataWrapperId;

    public PostgreForeignServer(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        super(database);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "srvname");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "srvowner");
        this.dataWrapperId = JDBCUtils.safeGetLong(dbResult, "srvfdw");
        this.type = JDBCUtils.safeGetString(dbResult, "srvtype");
        this.version = JDBCUtils.safeGetString(dbResult, "srvversion");
        this.options = JDBCUtils.safeGetArray(dbResult, "srvoptions");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 3)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 4)
    public String getVersion() {
        return version;
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

    @Property(viewable = true, order = 10)
    public PostgreForeignDataWrapper getForeignDataWrapper(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, getDatabase().foreignDataWrapperCache, getDatabase(), dataWrapperId);
    }

}
