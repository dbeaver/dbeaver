/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
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
        if (this.objectOwner == null) this.objectOwner = JDBCUtils.safeGetString(resultSet, "TABLE_SCHEMA");
        this.objectType = JDBCUtils.safeGetString(resultSet, "OBJECT_TYPE");
        this.privilege = JDBCUtils.safeGetString(resultSet, "PRIVILEGE");
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantable = JDBCUtils.safeGetBoolean(resultSet, "GRANTABLE", "Y");
        this.hierarchy = JDBCUtils.safeGetBoolean(resultSet, "HIERARCHY", "Y");
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    @Property(order = 4, viewable = true)
    public String getObjectType()
    {
        return objectType;
    }

    @Property(order = 5, viewable = true, supportsPreview = true)
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

    @Property(viewable = true, order = 10)
    public String getPrivilege()
    {
        return privilege;
    }

    @Property(order = 11)
    public String getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 12)
    public boolean isGrantable()
    {
        return grantable;
    }

    @Property(viewable = true, order = 13)
    public boolean isHierarchy()
    {
        return hierarchy;
    }
}
