/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

import java.util.Map;

/**
 * Query manager execution handler.
 * Handler methods are invoked right at time of DBC operation, so they should work as fast as possible.
 * Implementers should not invoke any DBC execution function in passed objects - otherwise execution handling may enter infinite recursion.
 */
public interface QMExecutionHandler {

    @NotNull
    String getHandlerName();

    void handleContextOpen(@NotNull DBCExecutionContext context, boolean transactional);

    void handleContextClose(@NotNull DBCExecutionContext context);

    void handleSessionOpen(@NotNull DBCSession session);

    void handleSessionClose(@NotNull DBCSession session);

    void handleTransactionAutocommit(@NotNull DBCExecutionContext context, boolean autoCommit);

    void handleTransactionIsolation(@NotNull DBCExecutionContext context, @NotNull DBPTransactionIsolation level);

    void handleTransactionCommit(@NotNull DBCExecutionContext context);

    void handleTransactionSavepoint(@NotNull DBCSavepoint savepoint);

    void handleTransactionRollback(@NotNull DBCExecutionContext context, @Nullable DBCSavepoint savepoint);

    void handleStatementOpen(@NotNull DBCStatement statement);

    void handleStatementExecuteBegin(@NotNull DBCStatement statement);

    void handleStatementExecuteEnd(@NotNull DBCStatement statement, long rows, Throwable error);

    void handleStatementBind(@NotNull DBCStatement statement, Object column, @Nullable Object value);

    void handleStatementClose(@NotNull DBCStatement statement, long rows);

    void handleResultSetOpen(@NotNull DBCResultSet resultSet);

    void handleResultSetClose(@NotNull DBCResultSet resultSet, long rowCount);

    void handleScriptBegin(@NotNull DBCSession session);
    
    void handleScriptEnd(@NotNull DBCSession session);

    void handleFeatureUsage(@NotNull DBRFeature feature, @Nullable Map<String, Object> parameters);
}
