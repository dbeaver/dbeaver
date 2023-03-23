/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.sql.SQLException;

/**
* Statement execute info
*/
public class QMMStatementExecuteInfo extends QMMObject {

    private final QMMStatementInfo statement;
    private QMMTransactionSavepointInfo savepoint;
    private final String queryString;

    private long fetchRowCount;
    private long updateRowCount = -1;

    private int errorCode;
    private String errorMessage;

    private long fetchBeginTime;
    private long fetchEndTime;

    private boolean transactional;

    private QMMStatementExecuteInfo previous;

    QMMStatementExecuteInfo(
        QMMStatementInfo statement,
        QMMTransactionSavepointInfo savepoint,
        String queryString,
        QMMStatementExecuteInfo previous,
        SQLDialect sqlDialect)
    {
        super(QMMetaObjectType.STATEMENT_EXECUTE_INFO);
        this.statement = statement;
        this.previous = previous;
        this.savepoint = savepoint;
        this.queryString = queryString;
        if (savepoint != null) {
            savepoint.setLastExecute(this);
        }
        if (sqlDialect != null && queryString != null) {
            this.transactional = statement.getPurpose() != DBCExecutionPurpose.META && sqlDialect.isTransactionModifyingQuery(queryString);
        } else {
            this.transactional = false;
        }
    }

    public QMMStatementExecuteInfo(long openTime, long closeTime, QMMStatementInfo stmt, String queryString, long rowCount, int errorCode, String errorMessage, long fetchBeginTime, long fetchEndTime, boolean transactional) {
        super(QMMetaObjectType.STATEMENT_EXECUTE_INFO, openTime, closeTime);
        this.statement = stmt;
        this.queryString = queryString;
        this.fetchRowCount = rowCount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.fetchBeginTime = fetchBeginTime;
        this.fetchEndTime = fetchEndTime;
        this.transactional = transactional;
    }

    private QMMStatementExecuteInfo(Builder builder) {
        super(QMMetaObjectType.STATEMENT_EXECUTE_INFO, builder.openTime, builder.closeTime);
        statement = builder.statement;
        savepoint = builder.savepoint;
        queryString = builder.queryString;
        fetchRowCount = builder.fetchRowCount;
        updateRowCount = builder.updateRowCount;
        errorCode = builder.errorCode;
        errorMessage = builder.errorMessage;
        fetchBeginTime = builder.fetchBeginTime;
        fetchEndTime = builder.fetchEndTime;
        transactional = builder.transactional;
        previous = builder.previous;
    }

    public static Builder builder() {
        return new Builder();
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
        this.updateRowCount = rowCount;
        if (!transactional) {
            this.transactional = this.updateRowCount >= 0;
        }
        super.close();
    }

    void beginFetch()
    {
        this.fetchBeginTime = getTimeStamp();
    }

    void endFetch(long rowCount)
    {
        this.fetchEndTime = getTimeStamp();
        this.fetchRowCount = rowCount;
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

    public long getFetchRowCount() {
        return fetchRowCount;
    }

    public long getUpdateRowCount()
    {
        return updateRowCount;
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
        return transactional || updateRowCount > 0;
    }

    public QMMStatementExecuteInfo getPrevious()
    {
        return previous;
    }

    @Override
    public String toString() {
        return '"' + queryString + '"';
    }

    @Override
    public String getText() {
        return queryString;
    }

    @Override
    public QMMetaObjectType getObjectType() {
        return QMMetaObjectType.STATEMENT_EXECUTE_INFO;
    }

    @Override
    public long getDuration() {
        if (!isClosed()) {
            return -1;
        }
        long execTime = getCloseTime() - getOpenTime();
        long fetchTime = isFetching() ? 0 : getFetchEndTime() - getFetchBeginTime();
        return execTime + fetchTime;
    }

    @Override
    public QMMConnectionInfo getConnection() {
        return statement.getConnection();
    }

    public static final class Builder {
        private QMMStatementInfo statement;
        private QMMTransactionSavepointInfo savepoint;
        private String queryString;
        private long fetchRowCount;
        private long updateRowCount;
        private int errorCode;
        private String errorMessage;
        private long openTime;
        private long closeTime;
        private long fetchBeginTime;
        private long fetchEndTime;
        private boolean transactional;
        private QMMStatementExecuteInfo previous;

        private Builder() {
        }

        public Builder setStatement(QMMStatementInfo statement) {
            this.statement = statement;
            return this;
        }

        public Builder setSavepoint(QMMTransactionSavepointInfo savepoint) {
            this.savepoint = savepoint;
            return this;
        }

        public Builder setQueryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder setFetchRowCount(long fetchRowCount) {
            this.fetchRowCount = fetchRowCount;
            return this;
        }

        public Builder setUpdateRowCount(long updateRowCount) {
            this.updateRowCount = updateRowCount;
            return this;
        }

        public Builder setErrorCode(int errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder setOpenTime(long openTime) {
            this.openTime = openTime;
            return this;
        }

        public Builder setCloseTime(long closeTime) {
            this.closeTime = closeTime;
            return this;
        }
        public Builder setFetchBeginTime(long fetchBeginTime) {
            this.fetchBeginTime = fetchBeginTime;
            return this;
        }

        public Builder setFetchEndTime(long fetchEndTime) {
            this.fetchEndTime = fetchEndTime;
            return this;
        }

        public Builder setTransactional(boolean transactional) {
            this.transactional = transactional;
            return this;
        }

        public Builder setPrevious(QMMStatementExecuteInfo previous) {
            this.previous = previous;
            return this;
        }

        public QMMStatementExecuteInfo build() {
            return new QMMStatementExecuteInfo(this);
        }
    }
}
