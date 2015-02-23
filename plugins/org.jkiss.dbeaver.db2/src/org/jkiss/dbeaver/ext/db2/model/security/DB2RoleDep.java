/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Role used by Users and Groups
 * 
 * @author Denis Forveille
 */
public class DB2RoleDep extends DB2Object<DB2Role> implements DBAPrivilege {

    DB2GrantorGranteeType granteeType;
    private DB2Grantee grantee;
    private Boolean admin;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2RoleDep(DB2Role role, ResultSet resultSet) throws DBException
    {
        super(role, JDBCUtils.safeGetString(resultSet, "GRANTEE"), true);

        this.granteeType = CommonUtils.valueOf(DB2GrantorGranteeType.class,
            JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTEETYPE"));
        String granteeName = JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTEE");
        switch (granteeType) {
        case U:
            this.grantee = getDataSource().getUser(VoidProgressMonitor.INSTANCE, granteeName);
            break;
        case G:
            this.grantee = getDataSource().getGroup(VoidProgressMonitor.INSTANCE, granteeName);
            break;
        case R:
            this.grantee = getDataSource().getRole(VoidProgressMonitor.INSTANCE, granteeName);
            break;

        default:
            break;
        }
        this.admin = JDBCUtils.safeGetBoolean(resultSet, "ADMIN", DB2YesNo.Y.name());
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(hidden = true)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 1)
    public DB2Grantee getGrantee()
    {
        return grantee;
    }

    @Property(viewable = true, order = 2)
    public DB2GrantorGranteeType getGranteeType()
    {
        return granteeType;
    }

    @Property(viewable = true, order = 3)
    public Boolean getAdmin()
    {
        return admin;
    }

}
