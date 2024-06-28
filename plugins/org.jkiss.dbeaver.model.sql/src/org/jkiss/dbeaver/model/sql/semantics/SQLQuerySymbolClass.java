/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;

public enum SQLQuerySymbolClass {
    UNKNOWN(SQLTokenType.T_OTHER),
    QUOTED(SQLTokenType.T_QUOTED),
    RESERVED(SQLTokenType.T_KEYWORD),
    STRING(SQLTokenType.T_STRING),
    CATALOG(SQLTokenType.T_SCHEMA),
    SCHEMA(SQLTokenType.T_SCHEMA),
    TABLE(SQLTokenType.T_TABLE),
    OBJECT(SQLTokenType.T_TABLE),
    TABLE_ALIAS(SQLTokenType.T_TABLE_ALIAS),
    COLUMN(SQLTokenType.T_COLUMN),
    COLUMN_DERIVED(SQLTokenType.T_COLUMN_DERIVED),
    COMPOSITE_FIELD(SQLTokenType.T_COMPOSITE_FIELD),
    SQL_BATCH_VARIABLE(SQLTokenType.T_SQL_VARIABLE),
    DBEAVER_VARIABLE(SQLTokenType.T_VARIABLE),
    DBEAVER_PARAMETER(SQLTokenType.T_PARAMETER),
    ERROR(SQLTokenType.T_SEMANTIC_ERROR);
    
    private final SQLTokenType tokenType;
    
    private SQLQuerySymbolClass(@NotNull SQLTokenType tokenType) {
        this.tokenType = tokenType;
    }

    @NotNull
    public SQLTokenType getTokenType() {
        return this.tokenType;
    }
}