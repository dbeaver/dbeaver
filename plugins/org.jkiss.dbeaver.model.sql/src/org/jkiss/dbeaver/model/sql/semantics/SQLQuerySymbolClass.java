/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.messages.ModelSQLMessages;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;

public enum SQLQuerySymbolClass {
    UNKNOWN(SQLTokenType.T_OTHER, ModelSQLMessages.model_sql_semantic_symbolClass_UNKNOWN),
    QUOTED(SQLTokenType.T_QUOTED, ModelSQLMessages.model_sql_semantic_symbolClass_QUOTED),
    RESERVED(SQLTokenType.T_KEYWORD, ModelSQLMessages.model_sql_semantic_symbolClass_RESERVED),
    STRING(SQLTokenType.T_STRING, ModelSQLMessages.model_sql_semantic_symbolClass_STRING),
    CATALOG(SQLTokenType.T_SCHEMA, ModelSQLMessages.model_sql_semantic_symbolClass_CATALOG),
    SCHEMA(SQLTokenType.T_SCHEMA, ModelSQLMessages.model_sql_semantic_symbolClass_SCHEMA),
    TABLE(SQLTokenType.T_TABLE, ModelSQLMessages.model_sql_semantic_symbolClass_TABLE),
    OBJECT(SQLTokenType.T_TABLE, ModelSQLMessages.model_sql_semantic_symbolClass_OBJECT),
    FUNCTION(SQLTokenType.T_FUNCTION, ModelSQLMessages.model_sql_semantic_symbolClass_FUNCTION),
    TABLE_ALIAS(SQLTokenType.T_TABLE_ALIAS, ModelSQLMessages.model_sql_semantic_symbolClass_TABLE_ALIAS),
    COLUMN(SQLTokenType.T_COLUMN, ModelSQLMessages.model_sql_semantic_symbolClass_COLUMN),
    COLUMN_DERIVED(SQLTokenType.T_COLUMN_DERIVED, ModelSQLMessages.model_sql_semantic_symbolClass_COLUMN_DERIVED),
    COMPOSITE_FIELD(SQLTokenType.T_COMPOSITE_FIELD, ModelSQLMessages.model_sql_semantic_symbolClass_COMPOSITE_FIELD),
    SQL_BATCH_VARIABLE(SQLTokenType.T_SQL_VARIABLE, ModelSQLMessages.model_sql_semantic_symbolClass_SQL_BATCH_VARIABLE),
    DBEAVER_VARIABLE(SQLTokenType.T_VARIABLE, ModelSQLMessages.model_sql_semantic_symbolClass_DBEAVER_VARIABLE),
    DBEAVER_PARAMETER(SQLTokenType.T_PARAMETER, ModelSQLMessages.model_sql_semantic_symbolClass_DBEAVER_PARAMETER),
    DBEAVER_COMMAND(SQLTokenType.T_CONTROL, ModelSQLMessages.model_sql_semantic_symbolClass_DBEAVER_COMMAND),
    ERROR(SQLTokenType.T_SEMANTIC_ERROR, ModelSQLMessages.model_sql_semantic_symbolClass_ERROR);
    
    private final SQLTokenType tokenType;
    private final String description;
    
    private SQLQuerySymbolClass(@NotNull SQLTokenType tokenType, String description) {
        this.tokenType = tokenType;
        this.description = description;
    }

    @NotNull
    public SQLTokenType getTokenType() {
        return this.tokenType;
    }

    @NotNull
    public String getDescription() {
        return this.description;
    }
}