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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.Date;

/**
 * PostgreUser
 */
public class PostgreUser implements DBAUser, DBPSaveableObject
{
    static final Log log = Log.getLog(PostgreUser.class);

    private PostgreDataSource dataSource;
    private int oid;
    private String userName;
    private boolean canCreateDatabase;
    private boolean superUser;
    private boolean canUpdateSystem;
    private boolean canReplicate;
    private Date expireTime;
    private String[] sessionDefaults;

    private boolean persisted;

    public PostgreUser(PostgreDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        if (resultSet != null) {
            this.persisted = true;
            this.oid = JDBCUtils.safeGetInt(resultSet, "usesysid");
            this.userName = JDBCUtils.safeGetString(resultSet, "usename");
            this.canCreateDatabase = JDBCUtils.safeGetBoolean(resultSet, "usecreatedb");
            this.superUser = JDBCUtils.safeGetBoolean(resultSet, "usesuper");
            this.canUpdateSystem = JDBCUtils.safeGetBoolean(resultSet, "usecatupd");
            this.canReplicate = JDBCUtils.safeGetBoolean(resultSet, "userepl");
            this.expireTime = JDBCUtils.safeGetTimestamp(resultSet, "valuntil");
            this.sessionDefaults = null;//JDBCUtils.safeGetBoolean(resultSet, "usename");
        } else {
            this.persisted = false;
            this.userName = "user";
            this.oid = -1;
        }
    }

    @Property(viewable = true, order = 1)
    public int getObjectId() {
        return this.oid;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName() {
        return userName;
    }

    public void setName(String userName)
    {
        this.userName = userName;
    }

    @Property(viewable = true, order = 20)
    public boolean isCanCreateDatabase() {
        return canCreateDatabase;
    }

    @Property(viewable = true, order = 21)
    public boolean isSuperUser() {
        return superUser;
    }

    @Property(viewable = true, order = 22)
    public boolean isCanUpdateSystem() {
        return canUpdateSystem;
    }

    @Property(viewable = true, order = 23)
    public boolean isCanReplicate() {
        return canReplicate;
    }

    @Property(viewable = true, order = 24)
    public Date getExpireTime() {
        return expireTime;
    }

    @Property(viewable = true, order = 25)
    public String[] getSessionDefaults() {
        return sessionDefaults;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
        DBUtils.fireObjectUpdate(this);
    }


}
