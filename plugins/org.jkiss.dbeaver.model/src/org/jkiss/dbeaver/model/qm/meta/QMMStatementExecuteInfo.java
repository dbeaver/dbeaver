/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
* Statement execute info
*/
public class QMMStatementExecuteInfo extends QMMObject {

    private QMMStatementInfo statement;
    private QMMTransactionSavepointInfo savepoint;
    private String queryString;

    private long fetchRowCount;
    private long updateRowCount = -1;

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
        final SQLDialect sqlDialect = statement.getConnection().getSQLDialect();
        if (sqlDialect != null && queryString != null) {
            this.transactional = statement.getPurpose() != DBCExecutionPurpose.META && sqlDialect.isTransactionModifyingQuery(queryString);
        } else {
            this.transactional = false;
        }
    }

    public QMMStatementExecuteInfo(long openTime, long closeTime, QMMStatementInfo stmt, String queryString, long rowCount, int errorCode, String errorMessage, long fetchBeginTime, long fetchEndTime, boolean transactional) {
        super(openTime, closeTime);
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
        super(builder.openTime, builder.closeTime);
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

    @Override
    public Map<String, Object> toMap() throws DBException {
        Map<String, Object> serializedInfo = new LinkedHashMap<>();
        serializedInfo.put("query", getQueryString());
        serializedInfo.put("statement", getStatement().toMap());
        serializedInfo.put("updateRowCount", getUpdateRowCount());
        serializedInfo.put("fetchRowCount", getFetchRowCount());
        serializedInfo.put("errorCode", getErrorCode());
        serializedInfo.put("errorMessage", getErrorMessage());
        serializedInfo.put("openTime", getOpenTime());
        serializedInfo.put("closeTime", getCloseTime());
        serializedInfo.put("fetchBeginTime", getFetchBeginTime());
        serializedInfo.put("fetchEndTime", getFetchEndTime());
        return serializedInfo;
    }

    public static QMMStatementExecuteInfo fromMap(Map<String, Object> objectMap) {
        String query = CommonUtils.toString(objectMap.get("query"));
        QMMStatementInfo statement = QMMStatementInfo.fromMap((Map<String, Object>) objectMap.get("statement"));
        long updateRowCount = CommonUtils.toLong(objectMap.get("updateRowCount"));
        long fetchRowCount = CommonUtils.toLong(objectMap.get("fetchRowCount"));
        int errorCode = CommonUtils.toInt(objectMap.get("errorCode"));
        String errorMessage = CommonUtils.toString(objectMap.get("errorMessage"));
        long openTime = CommonUtils.toLong(objectMap.get("openTime"));
        long closeTime = CommonUtils.toLong(objectMap.get("closeTime"));
        long fetchBeginTime = CommonUtils.toLong(objectMap.get("fetchBeginTime"));
        long fetchEndTime = CommonUtils.toLong(objectMap.get("fetchEndTime"));
        return builder()
            .setQueryString(query)
            .setStatement(statement)
            .setUpdateRowCount(updateRowCount)
            .setFetchRowCount(fetchRowCount)
            .setErrorCode(errorCode)
            .setErrorMessage(errorMessage)
            .setOpenTime(openTime)
            .setCloseTime(closeTime)
            .setFetchBeginTime(fetchBeginTime)
            .setFetchEndTime(fetchEndTime)
            .build();
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
