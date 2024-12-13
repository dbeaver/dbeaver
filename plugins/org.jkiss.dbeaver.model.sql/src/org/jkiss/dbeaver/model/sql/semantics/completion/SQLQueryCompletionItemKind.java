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
package org.jkiss.dbeaver.model.sql.semantics.completion;

public enum SQLQueryCompletionItemKind {
    UNKNOWN,
    /**
     * Keywords and alike
     */
    RESERVED,
    /**
     * Subquery correlation alias when its underlying source is not a simple table reference 
     * <p> (simple identifier)
     */
    SUBQUERY_ALIAS(true, false),
    /**
     * Column name when defined by the correlation or by the column alias
     * <p> (simple identifier or prefixed with subquery alias)
     */
    DERIVED_COLUMN_NAME(false, true),
    /**
     * Table name never referenced in the underlying contexts
     * <p> (simple identifier or fullname)
     */
    NEW_TABLE_NAME(true, false),
    /**
     * Table name already used in the underlying contexts
     * <p> (simple identifier or fullname)
     */
    USED_TABLE_NAME(true, false),
    /**
     * Table column name when derived from the real table 
     * <p> (simple identifier, fullname, alias-prefixed)
     * */
    TABLE_COLUMN_NAME(false, true),
    /**
     * Join condition based on the foreign key
     * <p> (expression, consisting of two column references)
     */
    JOIN_CONDITION(false, false);


    public final boolean isTableName;
    public final boolean isColumnName;


    SQLQueryCompletionItemKind() {
        this(false, false);
    }

    SQLQueryCompletionItemKind(boolean isTableName, boolean isColumnName) {
        this.isTableName = isTableName;
        this.isColumnName = isColumnName;
    }
}
    