package org.jkiss.dbeaver.model.dbc;

import java.sql.Connection;

/**
 * DBCConnector
 */
public interface DBCConnector
{
    Connection getConnection();
}
