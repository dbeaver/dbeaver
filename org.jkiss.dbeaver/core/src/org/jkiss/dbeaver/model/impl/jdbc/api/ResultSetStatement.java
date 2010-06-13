/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.dbc.DBCQueryPurpose;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Ref;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Array;
import java.sql.ResultSetMetaData;
import java.sql.ParameterMetaData;
import java.sql.RowId;
import java.sql.NClob;
import java.sql.SQLXML;
import java.sql.SQLWarning;
import java.sql.Connection;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.Reader;
import java.util.Calendar;
import java.net.URL;

/**
 * ResultSet container.
 * May be used as "fake" statement to wrap result sets returned by connection metadata or something.
 */
public class ResultSetStatement extends PreparedStatementManagable {

    private ResultSet resultSet;

    public ResultSetStatement(
        DBRProgressMonitor monitor,
        String title,
        String query,
        DBCQueryPurpose queryPurpose,
        ResultSet resultSet)
    {
        super(VoidStatement.INSTANCE, monitor, title, query, queryPurpose);
        this.resultSet = resultSet;
    }

    public ResultSet executeQuery()
        throws SQLException
    {
        return resultSet;
    }

}