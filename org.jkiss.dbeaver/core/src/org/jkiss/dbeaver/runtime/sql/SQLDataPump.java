package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.dbc.DBCResultSet;

/**
 * SQLQueryListener
 */
public interface SQLDataPump {

    void fetchStart(DBCResultSet resultSet);

    void fetchRow(DBCResultSet resultSet);

    void fetchEnd(DBCResultSet resultSet);

}