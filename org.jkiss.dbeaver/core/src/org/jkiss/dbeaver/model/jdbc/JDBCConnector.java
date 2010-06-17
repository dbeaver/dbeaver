/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.jdbc;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Connection;

/**
 * DBCConnector
 */
public interface JDBCConnector
{
    DBPDataSource getDataSource();

    Connection getConnection();

    JDBCExecutionContext openContext(DBRProgressMonitor monitor);

}
