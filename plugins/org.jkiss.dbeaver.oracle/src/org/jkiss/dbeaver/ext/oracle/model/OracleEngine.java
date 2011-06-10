/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * OracleEngine
 */
public class OracleEngine extends OracleInformation {

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

    public OracleEngine(OracleDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        super(dataSource);
        this.loadInfo(dbResult);
    }

    public OracleEngine(OracleDataSource dataSource, String name) {
        super(dataSource);
        this.name = name;
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ENGINE_NAME);
        this.description = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ENGINE_DESCRIPTION);
        this.support = Support.valueOf(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ENGINE_SUPPORT));
        this.supportsTransactions = "YES".equals(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ENGINE_SUPPORT_TXN));
        this.supportsXA = "YES".equals(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ENGINE_SUPPORT_XA));
        this.supportsSavepoints = "YES".equals(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ENGINE_SUPPORT_SAVEPOINTS));
    }

    @Property(name = "Engine", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Property(name = "Support", viewable = true, order = 3)
    public Support getSupport() {
        return support;
    }

    @Property(name = "Supports Transactions", viewable = true, order = 4)
    public boolean isSupportsTransactions()
    {
        return supportsTransactions;
    }

    @Property(name = "Supports XA", viewable = true, order = 5)
    public boolean isSupportsXA()
    {
        return supportsXA;
    }

    @Property(name = "Supports Savepoints", viewable = true, order = 6)
    public boolean isSupportsSavepoints()
    {
        return supportsSavepoints;
    }

}
