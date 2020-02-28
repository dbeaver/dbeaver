/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
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
        return owner.getDatabase().getRoleById(monitor, role);
    }

    @Property(viewable = true, order = 2)
    public PostgreRole getMember(DBRProgressMonitor monitor) throws DBException {
        return owner.getDatabase().getRoleById(monitor, member);
    }

    @Property(viewable = true, order = 3)
    public PostgreRole getGrantor(DBRProgressMonitor monitor) throws DBException {
        return owner.getDatabase().getRoleById(monitor, grantor);
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

