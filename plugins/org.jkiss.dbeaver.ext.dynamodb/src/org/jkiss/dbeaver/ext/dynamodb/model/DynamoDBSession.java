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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDBSession extends AbstractSession {

    private final DynamoDBExecutionContext context;

    public DynamoDBSession(
            @NotNull DynamoDBExecutionContext context,
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionPurpose purpose,
            @NotNull String task) {
        super(monitor, purpose, task);
        this.context = context;
    }

    @NotNull
    @Override
    public DBCExecutionContext getExecutionContext() {
        return context;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return context.getDataSource();
    }

    @NotNull
    @Override
    public DBCStatement prepareStatement(
            @NotNull DBCStatementType type,
            @NotNull String query,
            boolean scrollable,
            boolean updatable,
            boolean returnGeneratedKeys) throws DBCException {
        return new DynamoDBStatement(this, query);
    }

    @Override
    public void cancelBlock(@NotNull DBRProgressMonitor monitor, @Nullable Thread blockThread) throws DBException {
    }

    @Nullable
    public DynamoDbClient getDynamoClient() {
        return context.getDataSource().getDynamoClient();
    }

    @NotNull
    public DynamoDBDataSource getDynamoDataSource() {
        return context.getDataSource();
    }
}
