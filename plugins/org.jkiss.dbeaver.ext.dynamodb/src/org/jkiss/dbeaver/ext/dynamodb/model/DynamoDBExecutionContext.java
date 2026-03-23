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
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class DynamoDBExecutionContext
        extends AbstractExecutionContext<DynamoDBDataSource, DynamoDBDataSource> {

    private volatile boolean connected = true;

    public DynamoDBExecutionContext(@NotNull DynamoDBDataSource dataSource, @NotNull String purpose) {
        super(dataSource, purpose);
    }

    @Override
    public boolean isConnected() {
        return connected && getDataSource().getDynamoClient() != null;
    }

    @NotNull
    @Override
    public DBCSession openSession(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionPurpose purpose,
            @NotNull String task) {
        return new DynamoDBSession(this, monitor, purpose, task);
    }

    @Override
    public void checkContextAlive(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (getDataSource().getDynamoClient() == null) {
            throw new DBException("DynamoDB client is not connected");
        }
        try {
            getDataSource().getDynamoClient().listTables(b -> b.limit(1));
        } catch (Exception e) {
            throw new DBException("DynamoDB connection check failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void invalidateContext(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCInvalidatePhase phase) throws DBException {
        switch (phase) {
            case BEFORE_INVALIDATE:
                connected = false;
                break;
            case INVALIDATE:
                getDataSource().initialize(monitor);
                connected = true;
                break;
            case AFTER_INVALIDATE:
                break;
        }
    }

    @Override
    public void close() {
        connected = false;
        closeContext();
    }
}
