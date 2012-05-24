/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;

/**
 * OraclePrivRole
 */
public class OraclePrivRole extends OraclePriv implements DBSObjectLazy<OracleDataSource> {
    private Object role;
    private boolean defaultRole;

    public OraclePrivRole(OracleGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTED_ROLE"), resultSet);
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.role = this.name;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Property(name = "Role", viewable = true, order = 2, description = "Granted role", supportsPreview = true)
    public Object getRole(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null) {
            return role;
        }
        return OracleUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().roleCache, this, null);
    }

    @Property(name = "Default", viewable = true, order = 4, description = "Indicates whether the role is designated as a DEFAULT ROLE for the user")
    public boolean isDefaultRole()
    {
        return defaultRole;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return this.role;
    }

}