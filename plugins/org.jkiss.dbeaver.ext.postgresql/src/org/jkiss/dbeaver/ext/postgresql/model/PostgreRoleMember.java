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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreRoleMember
 */
public class PostgreRoleMember implements DBSObject {

    private final PostgreRole owner;
    private long role;
    private long member;
    private long grantor;
    private boolean adminOption;

    public PostgreRoleMember(PostgreRole owner, ResultSet dbResult)
        throws SQLException
    {
        this.owner = owner;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.role = JDBCUtils.safeGetLong(dbResult, "roleid");
        this.member = JDBCUtils.safeGetLong(dbResult, "member");
        this.grantor = JDBCUtils.safeGetLong(dbResult, "grantor");
        this.adminOption = JDBCUtils.safeGetBoolean(dbResult, "admin_option");
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return owner.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public String getName()
    {
        return String.valueOf(member);
    }

    @Property(viewable = true, order = 1)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, owner.getDatabase().roleCache, owner.getDatabase(), role);
    }

    @Property(viewable = true, order = 2)
    public PostgreRole getMember(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, owner.getDatabase().roleCache, owner.getDatabase(), member);
    }

    @Property(viewable = true, order = 3)
    public PostgreRole getGrantor(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, owner.getDatabase().roleCache, owner.getDatabase(), grantor);
    }

    @Property(viewable = true, order = 4)
    public boolean isAdminOption() {
        return adminOption;
    }

    @Override
    public String toString() {
        return getName();
    }

}

