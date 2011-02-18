/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.dbeaver.model.DBPDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBCConnector
 */
public interface JDBCConnector
{
    DBPDataSource getDataSource();

    Connection getConnection();

    Connection openIsolatedConnection() throws SQLException;

}
