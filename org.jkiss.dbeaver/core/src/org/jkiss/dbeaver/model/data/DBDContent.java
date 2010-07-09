/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;

/**
 * DBC LOB
 *
 * @author Serge Rider
 */
public interface DBDContent extends DBDValue {

    long getContentLength() throws DBCException;

    String getContentType() throws DBCException;

    //DBDContentStorage getContents(DBRProgressMonitor monitor) throws DBCException;

    /**
     * Update contents
     * @param monitor monitor
     * @param storage
     * @return
     * @throws DBException
     */
    boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
        throws DBException;

}
