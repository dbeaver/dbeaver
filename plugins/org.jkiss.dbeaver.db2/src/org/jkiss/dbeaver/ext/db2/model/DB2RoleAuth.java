/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.ext.db2.model.dict.DB2GrantorGranteeType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Role Authorisations
 *
 * @author Denis Forveille
 */
public class DB2RoleAuth extends DB2Object<DB2Role> implements DBAPrivilege {

    private DB2GrantorGranteeType granteeType;
    private String grantor;
    private DB2GrantorGranteeType grantorType;
    private Boolean admin;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2RoleAuth(DB2Role role, ResultSet resultSet)
    {
        // TODO DF: Bad should be GRANTEETYPE+GRANTEE
        super(role, JDBCUtils.safeGetString(resultSet, "GRANTEE"), true);

        this.granteeType = CommonUtils.valueOf(DB2GrantorGranteeType.class, JDBCUtils.safeGetString(resultSet, "GRANTEETYPE"));
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class, JDBCUtils.safeGetString(resultSet, "GRANTORTYPE"));
        this.admin = JDBCUtils.safeGetBoolean(resultSet, "ADMIN", DB2YesNo.Y.name());
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, editable = false)
    public String getName()
    {
        return super.getName();
    }

    public DB2GrantorGranteeType getGranteeType()
    {
        return granteeType;
    }

    @Property(viewable = true, editable = false)
    public DB2GrantorGranteeType getGranteeTypeDescription()
    {
        return granteeType;
    }

    @Property(viewable = true, editable = false)
    public String getGrantor()
    {
        return grantor;
    }

    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

    @Property(viewable = true, editable = false)
    public String getGrantorTypeDescription()
    {
        return grantorType.getDescription();
    }

    @Property(viewable = true, editable = false)
    public Boolean getAdmin()
    {
        return admin;
    }

}
