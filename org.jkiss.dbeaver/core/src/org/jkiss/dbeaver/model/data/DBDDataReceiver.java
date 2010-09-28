/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;

/**
 * Data reciever.
 * Used to recieve some resultset data.
 * Resultset can be a result of some query execution, cursor returned from stored procedure, generated keys result set, etc.
 */
public interface DBDDataReceiver {

    void fetchStart(DBCExecutionContext context, DBCResultSet resultSet)
        throws DBCException;

    void fetchRow(DBCExecutionContext context, DBCResultSet resultSet)
        throws DBCException;

    /**
     * Called after entire result set is fetched and closed
     * @throws DBCException on error  @param context
     */
    void fetchEnd(DBCExecutionContext context)
        throws DBCException;

}