/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Data reciever.
 * Used to recieve some resultset data.
 * Resultset can be a result of some query execution, cursor returned from stored procedure, generated keys result set, etc.
 */
public interface DBDDataReceiver {

    void fetchStart(DBRProgressMonitor monitor, DBCResultSet resultSet)
        throws DBCException;

    void fetchRow(DBRProgressMonitor monitor, DBCResultSet resultSet)
        throws DBCException;

    /**
     * Called after entire result set is fetched and closed
     * @throws DBCException on error
     * @param monitor
     */
    void fetchEnd(DBRProgressMonitor monitor)
        throws DBCException;

}