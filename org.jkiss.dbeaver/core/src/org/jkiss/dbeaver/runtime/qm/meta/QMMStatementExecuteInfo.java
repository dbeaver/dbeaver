/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import java.sql.SQLException;

/**
* Statement execute info
*/
public class QMMStatementExecuteInfo {

    private QMMStatementInfo statement;
    private QMMSavePointInfo savepoint;
    private String queryString;
    //private Map<Object, Object> parameters;

    private long rowCount;

    private int errorCode;
    private String errorMessage;

    private long beginTime;
    private long endTime;
    private long fetchBeginTime;
    private long fetchEndTime;

    private QMMStatementExecuteInfo previous;

    public QMMStatementExecuteInfo(QMMStatementInfo statement, QMMSavePointInfo savepoint, String queryString, QMMStatementExecuteInfo previous)
    {
        this.statement = statement;
        this.previous = previous;
        this.savepoint = savepoint;
        this.queryString = queryString;
        this.beginTime = System.currentTimeMillis();
    }

    void endExecution(long rowCount, Throwable error)
    {
        this.endTime = System.currentTimeMillis();
        if (error != null) {
            if (error instanceof SQLException) {
                this.errorCode = ((SQLException)error).getErrorCode();
            }
            this.errorMessage = error.getMessage();
        }
        this.rowCount = rowCount;
    }

    void beginFetch()
    {
        this.fetchBeginTime = System.currentTimeMillis();
    }

    void endFetch()
    {
        this.fetchEndTime = System.currentTimeMillis();
    }

    public QMMStatementInfo getStatement()
    {
        return statement;
    }

    public QMMSavePointInfo getSavepoint()
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

    public long getBeginTime()
    {
        return beginTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public long getFetchBeginTime()
    {
        return fetchBeginTime;
    }

    public long getFetchEndTime()
    {
        return fetchEndTime;
    }

    public QMMStatementExecuteInfo getPrevious()
    {
        return previous;
    }
}
