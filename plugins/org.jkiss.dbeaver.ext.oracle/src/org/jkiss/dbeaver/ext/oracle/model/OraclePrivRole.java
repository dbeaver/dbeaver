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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;

/**
 * OraclePrivRole
 */
public class OraclePrivRole extends OraclePriv implements DBSObjectLazy<OracleDataSource> {
    private Object role;
    private boolean defaultRole;

    public OraclePrivRole(OracleGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTED_ROLE"), resultSet);
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.role = this.name;
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    @Property(viewable = true, order = 2, supportsPreview = true)
    public Object getRole(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null) {
            return role;
        }
        return OracleUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().roleCache, this, null);
    }

    @Property(viewable = true, order = 4)
    public boolean isDefaultRole()
    {
        return defaultRole;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return this.role;
    }

}
