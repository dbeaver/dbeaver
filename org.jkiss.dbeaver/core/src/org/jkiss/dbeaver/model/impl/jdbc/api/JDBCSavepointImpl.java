/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;

import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Savepoint
 */
public class JDBCSavepointImpl implements DBCSavepoint, Savepoint {

    static final Log log = LogFactory.getLog(JDBCSavepointImpl.class);

    private JDBCConnectionImpl connection;
    private Savepoint original;

    public JDBCSavepointImpl(JDBCConnectionImpl connection, Savepoint savepoint)
    {
        this.connection = connection;
        this.original = savepoint;
    }

    public int getId()
    {
        try {
            return original.getSavepointId();
        }
        catch (SQLException e) {
            log.error(e);
            return 0;
        }
    }

    public String getName()
    {
        try {
            return original.getSavepointName();
        }
        catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    public JDBCExecutionContext getContext()
    {
        return connection;
    }

    public int getSavepointId()
        throws SQLException
    {
        return original.getSavepointId();
    }

    public String getSavepointName()
        throws SQLException
    {
        return original.getSavepointName();
    }

    public Savepoint getOriginal()
    {
        return original;
    }
}
