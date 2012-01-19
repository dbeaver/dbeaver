/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;
import org.jkiss.wmi.service.WMIService;

import java.util.List;

/**
 * WMI statement
 */
public class WMIStatement implements DBCStatement {
    private WMIExecutionContext context;
    private DBCStatementType type;
    private String query;
    private Object userData;
    private List<WMIObject> queryResult;
    private long firstRow;
    private long maxRows;

    public WMIStatement(WMIExecutionContext context, DBCStatementType type, String query)
    {
        this.context = context;
        this.type = type;
        this.query = query;
    }

    WMIService getService()
    {
        return context.getDataSource().getService();
    }

    public DBCExecutionContext getContext()
    {
        return context;
    }

    public String getQueryString()
    {
        return query;
    }

    public String getDescription()
    {
        return null;
    }

    public boolean executeStatement() throws DBCException
    {
        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(
                context.getProgressMonitor(),
                getService(),
                firstRow,
                maxRows);
            try {
                WMIService.initializeThread();
                getService().executeQuery(query, sink, WMIConstants.WBEM_FLAG_SEND_STATUS);
                sink.waitForFinish();
                queryResult = sink.getObjectList();
                return true;
            } finally {
                WMIService.unInitializeThread();
            }
        } catch (WMIException e) {
            throw new DBCException("Can't execute query '" + query + "'", e);
        }
    }

    public DBCResultSet openResultSet() throws DBCException
    {
        if (queryResult == null) {
            return null;
        }
        try {
            return new WMIResultSet(context, null, queryResult);
        } catch (WMIException e) {
            throw new DBCException(e);
        }
    }

    public DBCResultSet openGeneratedKeysResultSet() throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    public int getUpdateRowCount() throws DBCException
    {
        return 0;
    }

    public void close()
    {

    }

    public void setLimit(long offset, long limit) throws DBCException
    {
        this.firstRow = offset;
        this.maxRows = limit;
    }

    public DBSObject getDataContainer()
    {
        return null;
    }

    public void setDataContainer(DBSObject container)
    {
    }

    public Object getUserData()
    {
        return userData;
    }

    public void setUserData(Object userData)
    {
        this.userData = userData;
    }

    public void cancelBlock() throws DBException
    {
    }

}
