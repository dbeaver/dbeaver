/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;

/**
 * IResultSetProvider
 */
public interface IResultSetProvider extends IDataSourceProvider {

    /**
     * Source object of result set
     * @return source object or null
     */
    DBPNamedObject getResultSetSource();

    /**
     * Checks that this provider is ready to extract data
     * @return true if ready
     */
    boolean isReadyToRun();

    /**
     * Extracts data to specified receiver.
     * Data extraction begins immediately.
     * @param dataReceiver data receiver
     * @param offset first row number
     * @param maxRows number of rows
     * @return number of extracted rows
     */
    int extractData(DBCExecutionContext context, DBDDataReceiver dataReceiver, int offset, int maxRows)
        throws DBException;

}
