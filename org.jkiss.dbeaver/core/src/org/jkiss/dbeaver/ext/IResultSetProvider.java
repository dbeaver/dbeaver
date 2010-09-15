/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;

/**
 * IResultSetProvider
 */
public interface IResultSetProvider extends IDataSourceProvider {

    /**
     * Source object of result set
     * @return source object or null
     */
    DBPObject getResultSetSource();

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
     * Extract data to specified data receiver
     * @param dataReceiver data receiver
     * @param offset first row number
     * @param maxRows number of rows
     */
    void extractResultSetData(DBDDataReceiver dataReceiver, int offset, int maxRows);

}
