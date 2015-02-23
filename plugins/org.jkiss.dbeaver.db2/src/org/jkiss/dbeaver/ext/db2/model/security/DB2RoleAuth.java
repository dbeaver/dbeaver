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

import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Authorisations assigned to Roles
 * 
 * @author Denis Forveille
 */
public class DB2RoleAuth extends DB2Object<DB2Role> implements DBAPrivilege {

    private String grantor;
    private DB2GrantorGranteeType grantorType;
    private Boolean admin;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2RoleAuth(DB2Role role, ResultSet resultSet)
    {
        super(role, JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTEE"), true);

        this.grantor = JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class,
            JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTORTYPE"));
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
    public DB2Role getRole()
    {
        return parent;
    }

    @Property(viewable = true, order = 2)
    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

    @Property(viewable = true, order = 3)
    public String getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 4)
    public Boolean getAdmin()
    {
        return admin;
    }

}
