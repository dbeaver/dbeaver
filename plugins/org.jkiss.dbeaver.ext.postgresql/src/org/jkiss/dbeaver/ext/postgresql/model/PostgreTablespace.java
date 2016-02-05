/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * PostgreTablespace
 */
public class PostgreTablespace implements PostgreObject {

    private final PostgreDatabase database;
    private int oid;
    private String name;
    private int ownerId;
    private Object[] options;

    public PostgreTablespace(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetInt(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "spcname");
        this.ownerId = JDBCUtils.safeGetInt(dbResult, "spcowner");
        this.options = (Object[]) JDBCUtils.safeGetArray(dbResult, "spcoptions");
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @Override
    public int getObjectId() {
        return oid;
    }

    @Property(order = 2)
    public PostgreAuthId getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, database.authIdCache, database, ownerId);
    }

    @Property(order = 100)
    public Object[] getOptions() {
        return options;
    }
}

