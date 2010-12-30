/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPObject;

/**
 * DBCResultSet
 */
public interface DBCResultSet extends DBPObject
{
    DBCExecutionContext getContext();

    DBCStatement getSource();

    Object getColumnValue(int index) throws DBCException;

    boolean nextRow() throws DBCException;

    DBCResultSetMetaData getResultSetMetaData() throws DBCException;

    void close();
}
