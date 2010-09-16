/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
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
     * Checks that this result set provider is already running
     * @return true or false
     */
    boolean isRunning();

    /**
     * Checks that this provider is ready to extract data
     * @return true if ready
     */
    boolean isReadyToRun();

    /**
     * Says data provider to start data extraction to specified data receiver.
     * Implementor usually starts new job which pumps data to receiver.
     * @param dataReceiver data receiver
     * @param offset first row number
     * @param maxRows number of rows
     */
    void startDataExtraction(DBDDataReceiver dataReceiver, int offset, int maxRows);

    /**
     * Extracts data to specified receiver.
     * Data extraction begins immediately.
     * @param dataReceiver data receiver
     * @param offset first row number
     * @param maxRows number of rows
     */
    void extractData(DBCExecutionContext context, DBDDataReceiver dataReceiver, int offset, int maxRows)
        throws DBException;

}
