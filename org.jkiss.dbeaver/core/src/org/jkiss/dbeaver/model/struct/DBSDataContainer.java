/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;

import java.util.List;

/**
 * Data container.
 * Provides facilities to query object for data.
 * Any data container MUST support data read. Other function may be not supported (client can check it with {@link #getSupportedFeatures()}).
 */
public interface DBSDataContainer extends DBSObject {

    public static final int DATA_INSERT         = 1;
    public static final int DATA_UPDATE         = 2;
    public static final int DATA_DELETE         = 4;
    public static final int DATA_READ_SEGMENT   = 8;

    int getSupportedFeatures();

    int readData(
        DBCExecutionContext context,
        DBDDataReceiver dataReceiver,
        int firstRow,
        int maxRows)
        throws DBException;

    int insertData(
        DBCExecutionContext context,
        List<DBDColumnValue> columns,
        DBDDataReceiver keysReceiver)
        throws DBException;

    int updateData(
        DBCExecutionContext context,
        List<DBDColumnValue> keyColumns,
        List<DBDColumnValue> updateColumns,
        DBDDataReceiver keysReceiver)
        throws DBException;

    int deleteData(
        DBCExecutionContext context,
        List<DBDColumnValue> keyColumns)
        throws DBException;

}
