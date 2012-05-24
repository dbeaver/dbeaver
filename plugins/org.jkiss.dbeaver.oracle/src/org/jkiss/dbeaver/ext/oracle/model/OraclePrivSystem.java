/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * OraclePrivSystem
 */
public class OraclePrivSystem extends OraclePriv {
    private boolean defaultRole;

    public OraclePrivSystem(OracleGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIVILEGE"), resultSet);
    }

    @Override
    @Property(name = "Privilege", viewable = true, order = 2, description = "System privilege")
    public String getName() {
        return super.getName();
    }

}