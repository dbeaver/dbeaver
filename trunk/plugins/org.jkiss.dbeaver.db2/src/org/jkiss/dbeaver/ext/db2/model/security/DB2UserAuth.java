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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * DB2 User or Group Authorisations
 *
 * @author Denis Forveille
 */
public class DB2UserAuth extends DB2Object<DB2UserBase> implements DBAPrivilege {

    private String privilege;
    private Boolean grantable;
    private String objectType;
    private String objectName;
    private String objectSchema;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2UserAuth(DB2UserBase userOrGroup, ResultSet resultSet)
    {
        super(userOrGroup,
            JDBCUtils.safeGetString(resultSet, "PRIVILEGE") + " " +
            JDBCUtils.safeGetString(resultSet, "OBJECTTYPE") + ":" + JDBCUtils.safeGetStringTrimmed(resultSet, "OBJECTSCHEMA")
                + "." + JDBCUtils.safeGetString(resultSet, "OBJECTNAME"),
            true);

        this.privilege = JDBCUtils.safeGetString(resultSet, "PRIVILEGE");
        this.grantable = JDBCUtils.safeGetBoolean(resultSet, "GRANTABLE", DB2YesNo.Y.name());
        this.objectType = JDBCUtils.safeGetString(resultSet, "OBJECTTYPE");
        this.objectName = JDBCUtils.safeGetString(resultSet, "OBJECTNAME");
        this.objectSchema = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJECTSCHEMA");
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 2)
    public String getObjectType()
    {
        return objectType;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getObjectSchema()
    {
        return objectSchema;
    }

    @Property(viewable = true, editable = false, order = 4)
    public String getObjectName()
    {
        return objectName;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getPrivilege()
    {
        return privilege;
    }

    public Boolean getGrantable()
    {
        return grantable;
    }

}
