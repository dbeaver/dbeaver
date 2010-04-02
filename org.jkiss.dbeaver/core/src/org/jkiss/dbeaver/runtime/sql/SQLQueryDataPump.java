package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.DBException;

/**
 * SQLQueryListener
 */
public interface SQLQueryDataPump {

    void fetchStart(DBCResultSet resultSet)
        throws DBCException;

    void fetchRow(DBCResultSet resultSet)
        throws DBCException;

    void fetchEnd(DBCResultSet resultSet)
        throws DBCException;

}