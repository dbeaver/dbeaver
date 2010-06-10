/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * SQLQueryListener
 */
public interface ISQLQueryDataPump {

    void fetchStart(DBCResultSet resultSet, DBRProgressMonitor monitor)
        throws DBCException;

    void fetchRow(DBCResultSet resultSet, DBRProgressMonitor monitor)
        throws DBCException;

    /**
     * Called after entire result set is fetched and closed
     * @throws DBCException on error
     * @param monitor
     */
    void fetchEnd(DBRProgressMonitor monitor)
        throws DBCException;

}