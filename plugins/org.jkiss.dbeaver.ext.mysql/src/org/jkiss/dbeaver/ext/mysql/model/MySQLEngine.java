/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQLEngine
 */
public class MySQLEngine extends MySQLInformation {

    public static enum Support {
        YES,
        NO,
        DEFAULT,
        DISABLED
    }

    private String name;
    private String description;
    private Support support;
    private boolean supportsTransactions;
    private boolean supportsXA;
    private boolean supportsSavepoints;

    public MySQLEngine(MySQLDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        super(dataSource);
        this.loadInfo(dbResult);
    }

    public MySQLEngine(MySQLDataSource dataSource, String name) {
        super(dataSource);
        this.name = name;
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_NAME);
        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_DESCRIPTION);
        this.support = Support.valueOf(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT));
        this.supportsTransactions = "YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_TXN));
        this.supportsXA = "YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_XA));
        this.supportsSavepoints = "YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE_SUPPORT_SAVEPOINTS));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    @Property(viewable = true, order = 3)
    public Support getSupport() {
        return support;
    }

    @Property(viewable = true, order = 4)
    public boolean isSupportsTransactions()
    {
        return supportsTransactions;
    }

    @Property(viewable = true, order = 5)
    public boolean isSupportsXA()
    {
        return supportsXA;
    }

    @Property(viewable = true, order = 6)
    public boolean isSupportsSavepoints()
    {
        return supportsSavepoints;
    }

}
