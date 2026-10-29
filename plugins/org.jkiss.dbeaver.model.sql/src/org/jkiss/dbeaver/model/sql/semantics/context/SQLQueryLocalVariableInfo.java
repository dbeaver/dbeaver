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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolDefinition;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;

/**
 * Describes a locally declared variable or any variable-like entity represented by
 * {@link SQLDialect.LocalVariableKind}, including row values, row sources, and cursors.
 * Constructed by its semantic model and indexed in local scopes.
 */
public record SQLQueryLocalVariableInfo(
    @NotNull SQLQuerySymbolEntry declaration,
    @NotNull SQLDialect.LocalVariableKind kind,
    @NotNull SQLQueryExprType type,
    @Nullable SQLQueryRowsSourceModel rows,
    boolean parameter
) implements SQLQuerySymbolDefinition {
    public SQLQueryLocalVariableInfo {
        if (kind.canBeRowsSource || kind.canAccessFields) {
            if (rows == null) {
                throw new IllegalArgumentException("A row-valued binding requires a row source");
            }
        } else if (rows != null) {
            throw new IllegalArgumentException("Only row-valued bindings may carry a row source");
        }
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return switch (this.kind) {
            case ROWSET -> SQLQuerySymbolClass.TABLE;
            case ROW_REFERENCE -> SQLQuerySymbolClass.TABLE_ALIAS;
            case VALUE, ROW_VALUE, CURSOR, CURSOR_VALUE -> SQLQuerySymbolClass.SQL_BATCH_VARIABLE;
        };
    }
}
