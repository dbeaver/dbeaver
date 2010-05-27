/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;

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