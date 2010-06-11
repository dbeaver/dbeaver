/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import java.sql.Connection;

/**
 * DBCConnector
 */
public interface JDBCConnector
{
    Connection getConnection();
}
