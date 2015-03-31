/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;
import org.jkiss.wmi.service.WMIService;

import java.util.List;

/**
 * WMI statement
 */
public class WMIStatement implements DBCStatement {
    private WMISession session;
    private DBCStatementType type;
    private String query;
    private List<WMIObject> queryResult;
    private long firstRow;
    private long maxRows;

    public WMIStatement(WMISession session, DBCStatementType type, String query)
    {
        this.session = session;
        this.type = type;
        this.query = query;
    }

    WMIService getService()
    {
        return session.getDataSource().getService();
    }

    @NotNull
    @Override
    public DBCSession getSession()
    {
        return session;
    }

    @Nullable
    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public boolean executeStatement() throws DBCException
    {
        try {
            WMIObjectCollectorSink sink = new WMIObjectCollectorSink(
                session.getProgressMonitor(),
                getService(),
                firstRow,
                maxRows);
            getService().executeQuery(query, sink, WMIConstants.WBEM_FLAG_SEND_STATUS);
            sink.waitForFinish();
            queryResult = sink.getObjectList();
            return true;
        } catch (WMIException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public void addToBatch() throws DBCException
    {
        throw new DBCException("Batches not supported");
    }

    @Override
    public int[] executeStatementBatch() throws DBCException
    {
        throw new DBCException("Batches not supported");
    }

    @Nullable
    @Override
    public DBCResultSet openResultSet() throws DBCException
    {
        if (queryResult == null) {
            return null;
        }
        try {
            return new WMIResultSet(session, null, queryResult);
        } catch (WMIException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Nullable
    @Override
    public DBCResultSet openGeneratedKeysResultSet() throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    @Override
    public int getUpdateRowCount() throws DBCException
    {
        return -1;
    }

    @Override
    public boolean nextResults() throws DBCException {
        return false;
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

    @Nullable
    @Override
    public Object getStatementSource()
    {
        return null;
    }

    @Override
    public void setStatementSource(Object source)
    {
    }

    @Override
    public void cancelBlock() throws DBException
    {
    }

}
