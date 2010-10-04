/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import java.sql.SQLException;

/**
* Statement execute info
*/
public class QMMStatementExecuteInfo extends QMMObject {

    private QMMStatementInfo statement;
    private QMMSavepointInfo savepoint;
    private String queryString;
    //private Map<Object, Object> parameters;

    private long rowCount;

    private int errorCode;
    private String errorMessage;

    private long fetchBeginTime;
    private long fetchEndTime;

    private QMMStatementExecuteInfo previous;

    public QMMStatementExecuteInfo(QMMStatementInfo statement, QMMSavepointInfo savepoint, String queryString, QMMStatementExecuteInfo previous)
    {
        this.statement = statement;
        this.previous = previous;
        this.savepoint = savepoint;
        this.queryString = queryString;
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

    public QMMSavepointInfo getSavepoint()
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
