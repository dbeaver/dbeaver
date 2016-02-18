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
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * OraclePrivRole
 */
public abstract class OraclePriv extends OracleObject<OracleGrantee> implements DBAPrivilege {
    private boolean adminOption;

    public OraclePriv(OracleGrantee user, String name, ResultSet resultSet) {
        super(user, name, true);
        this.adminOption = JDBCUtils.safeGetBoolean(resultSet, "ADMIN_OPTION", "Y");
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    @Property(viewable = true, order = 3)
    public boolean isAdminOption()
    {
        return adminOption;
    }

}
