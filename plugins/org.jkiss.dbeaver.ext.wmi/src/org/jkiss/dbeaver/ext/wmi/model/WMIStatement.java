/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
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
    private DBCExecutionSource source;

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
            throw new DBCException(e, session.getExecutionContext());
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
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Nullable
    @Override
    public DBCResultSet openGeneratedKeysResultSet() throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    @Override
    public long getUpdateRowCount() throws DBCException
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
    public Throwable[] getStatementWarnings() throws DBCException {
        return null;
    }

    @Override
    public void setStatementTimeout(int timeout) throws DBCException {

    }

    @Override
    public void setResultsFetchSize(int fetchSize) throws DBCException {
        // not supported
    }

    @Nullable
    @Override
    public DBCExecutionSource getStatementSource()
    {
        return source;
    }

    @Override
    public void setStatementSource(DBCExecutionSource source)
    {
        this.source = source;
    }

    @Override
    public void cancelBlock(@NotNull DBRProgressMonitor monitor, @Nullable Thread blockThread) throws DBException
    {
    }

}
