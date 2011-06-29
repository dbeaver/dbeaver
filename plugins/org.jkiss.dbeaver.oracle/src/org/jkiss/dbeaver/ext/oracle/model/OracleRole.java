/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * OracleRole
 */
public class OracleRole extends OracleGlobalObject
{
    static final Log log = LogFactory.getLog(OracleRole.class);

    private String name;
    private String authentication;

    public OracleRole(OracleDataSource dataSource, ResultSet resultSet) {
        super(dataSource, true);
        this.name = JDBCUtils.safeGetString(resultSet, "ROLE");
        this.authentication = JDBCUtils.safeGetStringTrimmed(resultSet, "PASSWORD_REQUIRED");
    }

    @Property(name = "Role name", viewable = true, order = 2, description = "Name of the role")
    public String getName() {
        return name;
    }

    @Property(name = "Authentication", viewable = true, order = 3, description = "Indicates if the role requires a password to be enabled")
    public String getAuthentication()
    {
        return authentication;
    }

}