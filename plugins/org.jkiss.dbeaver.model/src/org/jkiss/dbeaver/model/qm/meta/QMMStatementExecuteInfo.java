/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.sql.SQLException;

/**
* Statement execute info
*/
public class QMMStatementExecuteInfo extends QMMObject {

    private QMMStatementInfo statement;
    private QMMTransactionSavepointInfo savepoint;
    private String queryString;

    private long rowCount;

    private int errorCode;
    private String errorMessage;

    private long fetchBeginTime;
    private long fetchEndTime;

    private boolean transactional;

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
        final SQLDialect sqlDialect = statement.getSession().getSQLDialect();
        if (sqlDialect != null && queryString != null) {
            this.transactional = sqlDialect.isTransactionModifyingQuery(queryString);
        } else {
            this.transactional = true;
        }

    }

    public QMMStatementExecuteInfo(long openTime, long closeTime, QMMStatementInfo stmt, String queryString, long rowCount, int errorCode, String errorMessage, long fetchBeginTime, long fetchEndTime, boolean transactional) {
        super(openTime, closeTime);
        this.statement = stmt;
        this.queryString = queryString;
        this.rowCount = rowCount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.fetchBeginTime = fetchBeginTime;
        this.fetchEndTime = fetchEndTime;
        this.transactional = transactional;
    }

    void close(long rowCount, Throwable error)
    {
        if (error != null) {
            if (error instanceof SQLException) {
                this.errorCode = ((SQLException)error).getErrorCode();
            }
            this.errorMessage = error.getMessage();
            // SQL error makes ANY statement transactional (PG specific?)
            this.transactional = true;
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

    public boolean isTransactional() {
        return transactional;
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

    @Override
    public String getText() {
        return queryString;
    }
}
