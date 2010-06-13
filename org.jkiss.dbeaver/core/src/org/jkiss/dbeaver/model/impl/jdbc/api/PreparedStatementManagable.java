/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.dbc.DBCQueryPurpose;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.DBException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Managable prepared statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class PreparedStatementManagable extends PreparedStatementWrapper implements DBRBlockingObject {

    private DBRProgressMonitor monitor;
    private String title;
    private String query;
    private DBCQueryPurpose queryPurpose;

    public PreparedStatementManagable(
        PreparedStatement original,
        DBRProgressMonitor monitor,
        String title,
        String query,
        DBCQueryPurpose queryPurpose)
    {
        super(original);
        this.monitor = monitor;
        this.title = title;
        this.query = query;
        this.queryPurpose = queryPurpose;

        this.monitor.startBlock(this, title);
    }

    @Override
    public void close()
        throws SQLException
    {
        try {
            super.close();
        }
        finally {
            monitor.endBlock();
        }
    }

    public void cancelBlock()
        throws DBException
    {
        try {
            this.cancel();
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
    }

}
