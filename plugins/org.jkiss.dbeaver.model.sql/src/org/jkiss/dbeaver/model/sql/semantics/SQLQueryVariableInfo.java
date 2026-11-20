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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLScriptVariableScope;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;

/**
 * Lightweight variable information retained independently of a parsed query model.
 */
public record SQLQueryVariableInfo(
    @NotNull String rawName,
    @NotNull String canonicalName,
    @NotNull SQLQueryExprType type,
    @NotNull SQLScriptVariableScope scope,
    @NotNull OperationKind operationKind,
    int relativeOffset
) implements SQLQuerySymbolDefinition {
    public enum OperationKind {
        ASSIGNMENT,
        DECLARATION
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return SQLQuerySymbolClass.SQL_BATCH_VARIABLE;
    }

}
