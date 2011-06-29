/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;

/**
 * OracleRolePriv
 */
public class OracleRolePriv extends OracleObject<OracleUser> implements DBSObjectLazy<OracleDataSource> {
    private Object role;
    private boolean adminOption;
    private boolean defaultRole;

    public OracleRolePriv(OracleUser user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTED_ROLE"), true);
        this.adminOption = JDBCUtils.safeGetBoolean(resultSet, "ADMIN_OPTION", "Y");
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.role = this.name;
    }

    public String getName() {
        return super.getName();
    }

    @Property(name = "Role", viewable = true, order = 2, description = "Granted role", supportsPreview = true)
    public Object getRole(DBRProgressMonitor monitor) throws DBException
    {
        return monitor == null ?
            role :
            OracleUtils.resolveLazyReference(monitor, getParentObject(), getParentObject().rolePrivCache, this, null);
    }

    @Property(name = "Admin Option", viewable = true, order = 3, description = "Indicates whether the grant was with the ADMIN OPTION")
    public boolean isAdminOption()
    {
        return adminOption;
    }

    @Property(name = "Default", viewable = true, order = 4, description = "Indicates whether the role is designated as a DEFAULT ROLE for the user")
    public boolean isDefaultRole()
    {
        return defaultRole;
    }

    public Object getLazyReference(Object propertyId)
    {
        return this.role;
    }

}