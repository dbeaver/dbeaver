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

package org.jkiss.dbeaver.runtime.qm.meta;

import java.sql.SQLException;

/**
* Statement execute info
*/
public class QMMStatementExecuteInfo extends QMMObject {

    private QMMStatementInfo statement;
    private QMMTransactionSavepointInfo savepoint;
    private String queryString;
    //private Map<Object, Object> parameters;

    private long rowCount;

    private int errorCode;
    private String errorMessage;

    private long fetchBeginTime;
    private long fetchEndTime;

    private QMMStatementExecuteInfo previous;

    QMMStatementExecuteInfo(QMMStatementInfo statement, QMMTransactionSavepointInfo savepoint, String queryString, QMMStatementExecuteInfo previous)
    {
        this.statement = statement;
        this.previous = previous;
        this.savepoint = savepoint;
        this.queryString = queryString;
        if (savepoint != null) {
            savepoint.setLastExecute(this);
        }
    }

    void close(long rowCount, Throwable error)
    {
        if (error != null) {
            if (error instanceof SQLException) {
                this.errorCode = ((SQLException)error).getErrorCode();
            }
            this.errorMessage = error.getMessage();
        }
        this.rowCount = rowCount;
        super.close();
    }

    void beginFetch()
    {
        this.fetchBeginTime = getTimeStamp();
    }

    void endFetch(long rowCount)
    {
        this.fetchEndTime = getTimeStamp();
        this.rowCount = rowCount;
    }

    public QMMStatementInfo getStatement()
    {
        return statement;
    }

    public QMMTransactionSavepointInfo getSavepoint()
    {
        return savepoint;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public int getErrorCode()
    {
        return errorCode;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public boolean hasError()
    {
        return errorCode != 0 || errorMessage != null;
    }

    public long getFetchBeginTime()
    {
        return fetchBeginTime;
    }

    public long getFetchEndTime()
    {
        return fetchEndTime;
    }

    public boolean isFetching()
    {
        return fetchBeginTime > 0 && fetchEndTime == 0;
    }

    public QMMStatementExecuteInfo getPrevious()
    {
        return previous;
    }

    @Override
    public String toString()
    {
        return '"' + queryString + '"';
    }

}
