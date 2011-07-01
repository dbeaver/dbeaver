/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

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

    public String getName() {
        return super.getName();
    }

    @Property(name = "Admin Option", viewable = true, order = 3, description = "Indicates whether the grant was with the ADMIN OPTION")
    public boolean isAdminOption()
    {
        return adminOption;
    }

}