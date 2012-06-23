/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * OraclePrivObject
 */
public class OraclePrivObject extends OracleObject<OracleGrantee> implements DBAPrivilege {
    private String objectOwner;
    private String objectType;
    private String privilege;
    private String grantor;
    private boolean grantable;
    private boolean hierarchy;

    public OraclePrivObject(OracleGrantee grantee, ResultSet resultSet) {
        super(grantee, JDBCUtils.safeGetString(resultSet, "TABLE_NAME"), true);
        this.objectOwner = JDBCUtils.safeGetString(resultSet, "OWNER");
        this.objectType = JDBCUtils.safeGetString(resultSet, "OBJECT_TYPE");
        this.privilege = JDBCUtils.safeGetString(resultSet, "PRIVILEGE");
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantable = JDBCUtils.safeGetBoolean(resultSet, "GRANTABLE", "Y");
        this.hierarchy = JDBCUtils.safeGetBoolean(resultSet, "HIERARCHY", "Y");
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Property(name = "Object Type", order = 4, viewable = true, description = "Object type")
    public String getObjectType()
    {
        return objectType;
    }

    @Property(name = "Object", order = 5, viewable = true, supportsPreview = true, description = "The object can be any object, including tables, packages, indexes, sequences, and so on")
    public Object getObject(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null || CommonUtils.isEmpty(objectOwner)) {
            return name;
        }
        return OracleObjectType.resolveObject(
            monitor,
            getDataSource(),
            null,
            objectType,
            objectOwner,
            name);
    }

    @Property(name = "Privilege", viewable = true, order = 10, description = "Privilege on the object")
    public String getPrivilege()
    {
        return privilege;
    }

    @Property(name = "Grantor", order = 11, description = "User who performed the grant")
    public String getGrantor()
    {
        return grantor;
    }

    @Property(name = "Grantable", viewable = true, order = 12, description = "Indicates whether the privilege was granted with the GRANT OPTION")
    public boolean isGrantable()
    {
        return grantable;
    }

    @Property(name = "Hierarchy", viewable = true, order = 13, description = "Indicates whether the privilege was granted with the HIERARCHY OPTION")
    public boolean isHierarchy()
    {
        return hierarchy;
    }
}