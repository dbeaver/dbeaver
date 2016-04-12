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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * PostgreAuthId
 */
public class PostgreAuthId implements PostgreObject {

    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_FLAGS = "Flags";

    private final PostgreDatabase database;
    private long oid;
    private String name;
    private boolean superUser;
    private boolean inherit;
    private boolean createRole;
    private boolean createDatabase;
    private boolean canLogin;
    private boolean replication;
    private boolean bypassRls;
    private int connLimit;
    private String password;
    private Date validUntil;

    public PostgreAuthId(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "rolname");
        this.superUser = JDBCUtils.safeGetBoolean(dbResult, "rolsuper");
        this.inherit = JDBCUtils.safeGetBoolean(dbResult, "rolinherit");
        this.createRole = JDBCUtils.safeGetBoolean(dbResult, "rolcreaterole");
        this.createDatabase = JDBCUtils.safeGetBoolean(dbResult, "rolcreatedb");
        this.canLogin = JDBCUtils.safeGetBoolean(dbResult, "rolcanlogin");
        this.replication = JDBCUtils.safeGetBoolean(dbResult, "rolreplication");
        this.bypassRls = JDBCUtils.safeGetBoolean(dbResult, "rolbypassrls");
        this.connLimit = JDBCUtils.safeGetInt(dbResult, "rolconnlimit");
        this.password = JDBCUtils.safeGetString(dbResult, "rolpassword");
        this.validUntil = JDBCUtils.safeGetTimestamp(dbResult, "rolvaliduntil");
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
    public PostgreDataSource getDataSource() {
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

    @Property(viewable = true, order = 2)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(category = CAT_FLAGS, order = 10)
    public boolean isSuperUser() {
        return superUser;
    }

    @Property(category = CAT_FLAGS, order = 11)
    public boolean isInherit() {
        return inherit;
    }

    @Property(category = CAT_FLAGS, order = 12)
    public boolean isCreateRole() {
        return createRole;
    }

    @Property(category = CAT_FLAGS, order = 13)
    public boolean isCreateDatabase() {
        return createDatabase;
    }

    @Property(category = CAT_FLAGS, order = 14)
    public boolean isCanLogin() {
        return canLogin;
    }

    @Property(category = CAT_FLAGS, order = 15)
    public boolean isReplication() {
        return replication;
    }

    @Property(category = CAT_FLAGS, order = 16)
    public boolean isBypassRls() {
        return bypassRls;
    }

    @Property(category = CAT_SETTINGS, order = 20)
    public int getConnLimit() {
        return connLimit;
    }

    @Property(hidden = true, category = CAT_SETTINGS, order = 21)
    public String getPassword() {
        return password;
    }

    @Property(category = CAT_SETTINGS, order = 22)
    public Date getValidUntil() {
        return validUntil;
    }
}

