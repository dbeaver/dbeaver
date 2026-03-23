/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementResponse;

import java.util.ArrayList;
import java.util.List;

public class DynamoDBStatement implements DBCStatement {

    private final DynamoDBSession session;
    private final String query;
    private DBCExecutionSource source;
    private DynamoDBResultSet resultSet;
    private long updateCount = -1;
    private long limitRows = -1;
    private final List<DBPCloseableObject> closeListeners = new ArrayList<>();

    public DynamoDBStatement(@NotNull DynamoDBSession session, @NotNull String query) {
        this.session = session;
        this.query = query;
    }

    @NotNull
    @Override
    public DBCSession getSession() {
        return session;
    }

    @Nullable
    @Override
    public String getQueryString() {
        return query;
    }

    @Nullable
    @Override
    public DBCExecutionSource getStatementSource() {
        return source;
    }

    @Override
    public void setStatementSource(@Nullable DBCExecutionSource source) {
        this.source = source;
    }

    @Override
    public boolean executeStatement() throws DBCException {
        if (session.getDynamoClient() == null) {
            throw new DBCException("DynamoDB client is not connected");
        }
        try {
            String trimmed = query.trim().toUpperCase();
            boolean isSelect = trimmed.startsWith("SELECT") || trimmed.startsWith("SCAN");

            ExecuteStatementRequest.Builder reqBuilder = ExecuteStatementRequest.builder()
                    .statement(query);
            if (isSelect && limitRows > 0) {
                reqBuilder.limit((int) limitRows);
            }

            ExecuteStatementResponse resp = session.getDynamoClient()
                    .executeStatement(reqBuilder.build());

            if (isSelect) {
                this.resultSet = new DynamoDBResultSet(session, this, resp.items());
                this.updateCount = -1;
                return true;
            } else {
                this.resultSet = null;
                this.updateCount = 1;
                return false;
            }
        } catch (DynamoDbException e) {
            throw new DBCException("Error executing DynamoDB statement: " + e.getMessage(), e);
        }
    }

    @Nullable
    @Override
    public DBCResultSet openResultSet() throws DBCException {
        return resultSet;
    }

    @Override
    public long getUpdateRowCount() throws DBCException {
        return updateCount;
    }

    @Override
    public boolean nextResults() throws DBCException {
        return false;
    }

    @Override
    public void setLimit(long offset, long limit) throws DBCException {
        this.limitRows = limit;
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
    }

    @Override
    public void close() {
        for (DBPCloseableObject listener : closeListeners) {
            try {
                listener.close();
            } catch (Exception e) {
            }
        }
        closeListeners.clear();
        resultSet = null;
    }

    @Override
    public void cancelBlock(@NotNull DBRProgressMonitor monitor, @Nullable Thread blockThread) throws DBException {
    }

    @Override
    public void autoCloseDependant(@NotNull DBPCloseableObject dependent) {
        closeListeners.add(dependent);
    }
}
