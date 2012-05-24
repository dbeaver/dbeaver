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

    @Override
    public DBCExecutionContext getContext()
    {
        return context;
    }

    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public boolean executeStatement() throws DBCException
    {
        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(
                context.getProgressMonitor(),
                getService(),
                firstRow,
                maxRows);
            getService().executeQuery(query, sink, WMIConstants.WBEM_FLAG_SEND_STATUS);
            sink.waitForFinish();
            queryResult = sink.getObjectList();
            return true;
        } catch (WMIException e) {
            throw new DBCException("Can't execute query '" + query + "'", e);
        }
    }

    @Override
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

    @Override
    public DBCResultSet openGeneratedKeysResultSet() throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    @Override
    public int getUpdateRowCount() throws DBCException
    {
        return 0;
    }

    @Override
    public void close()
    {

    }

    @Override
    public void setLimit(long offset, long limit) throws DBCException
    {
        this.firstRow = offset;
        this.maxRows = limit;
    }

    @Override
    public DBSObject getDataContainer()
    {
        return null;
    }

    @Override
    public void setDataContainer(DBSObject container)
    {
    }

    @Override
    public Object getUserData()
    {
        return userData;
    }

    @Override
    public void setUserData(Object userData)
    {
        this.userData = userData;
    }

    @Override
    public void cancelBlock() throws DBException
    {
    }

}
