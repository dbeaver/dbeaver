/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.dbc.DBCException;

import java.sql.Statement;

/**
 * Managable base statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class BaseStatementManagable extends StatementManagable {

    private Statement original;

    public BaseStatementManagable(
        ConnectionManagable connection,
        Statement original)
    {
        super(connection);
        this.original = original;
    }

    protected Statement getOriginal()
    {
        return original;
    }

    ////////////////////////////////////////////////////////////////////
    // DBC Statement overrides
    ////////////////////////////////////////////////////////////////////

    public boolean executeStatement()
        throws DBCException
    {
        throw new DBCException("Operation is not supported");
    }

}