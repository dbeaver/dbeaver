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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Base class for DB2 User or Group Authorisations
 * 
 * @author Denis Forveille
 */
public abstract class DB2UserAuthBase extends DB2Object<DB2UserBase> implements DBAPrivilege {

    private DBSObject grantor;
    private DB2GrantorGranteeType grantorType;

    private DB2Schema objectSchema;
    private DBSObject object;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2UserAuthBase(DBRProgressMonitor monitor, DB2UserBase userOrGroup, DBSObject object, ResultSet resultSet)
        throws DBException
    {
        super(userOrGroup, JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA") + "."
            + JDBCUtils.safeGetString(resultSet, "OBJ_NAME"), true);

        DB2DataSource db2DataSource = userOrGroup.getDataSource();
        String objectSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA");
        if (objectSchemaName != null) {
            this.objectSchema = db2DataSource.getSchema(monitor, objectSchemaName);
        }

        this.object = object;

        String grantorName = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class, JDBCUtils.safeGetString(resultSet, "GRANTORTYPE"));
        switch (grantorType) {
        case U:
            this.grantor = userOrGroup.getDataSource().getUser(monitor, grantorName);
            break;
        case G:
            this.grantor = userOrGroup.getDataSource().getGroup(monitor, grantorName);
            break;
        default:
            break;
        }

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
    public DB2Schema getObjectSchema()
    {
        return objectSchema;
    }

    @Property(viewable = true, order = 2)
    public DBSObject getObject()
    {
        return object;
    }

    @Property(viewable = true, order = 3)
    public DBSObject getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 4)
    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

}
