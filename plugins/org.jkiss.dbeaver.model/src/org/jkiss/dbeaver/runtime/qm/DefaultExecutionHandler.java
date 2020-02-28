/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.Map;

/**
 * Default execution handler.
 * Handle methods are no-ops.
 */
public abstract class DefaultExecutionHandler implements QMExecutionHandler {

    @Override
    public void handleContextOpen(@NotNull DBCExecutionContext context, boolean transactional)
    {

    }

    @Override
    public void handleContextClose(@NotNull DBCExecutionContext context)
    {

    }

    @Override
    public void handleSessionOpen(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleSessionClose(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleTransactionAutocommit(@NotNull DBCExecutionContext context, boolean autoCommit)
    {

    }

    @Override
    public void handleTransactionIsolation(@NotNull DBCExecutionContext context, @NotNull DBPTransactionIsolation level)
    {

    }

    @Override
    public void handleTransactionCommit(@NotNull DBCExecutionContext context)
    {

    }

    @Override
    public void handleTransactionSavepoint(@NotNull DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleTransactionRollback(@NotNull DBCExecutionContext context, DBCSavepoint savepoint)
    {

    }

    @Override
    public void handleStatementOpen(@NotNull DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteBegin(@NotNull DBCStatement statement)
    {

    }

    @Override
    public void handleStatementExecuteEnd(@NotNull DBCStatement statement, long rows, Throwable error)
    {
        
    }

    @Override
    public void handleStatementBind(@NotNull DBCStatement statement, Object column, Object value)
    {

    }

    @Override
    public void handleStatementClose(@NotNull DBCStatement statement, long rows)
    {

    }

    @Override
    public void handleResultSetOpen(@NotNull DBCResultSet resultSet)
    {

    }

    @Override
    public void handleResultSetClose(@NotNull DBCResultSet resultSet, long rowCount)
    {

    }

    @Override
    public void handleScriptBegin(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleScriptEnd(@NotNull DBCSession session)
    {

    }

    @Override
    public void handleFeatureUsage(@NotNull DBRFeature feature, @Nullable Map<String, Object> parameters) {

    }
}
