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
package org.jkiss.dbeaver.model.sql.semantics.completion;

public enum SQLQueryCompletionItemKind {
    UNKNOWN(false, false, Integer.MAX_VALUE),
    /**
     * Keywords and alike
     */
    RESERVED(false, false, 400),
    /**
     * Subquery correlation alias when its underlying source is not a simple table reference 
     * <p> (simple identifier)
     */
    SUBQUERY_ALIAS(true, false, 200),
    /**
     * Column name when defined by the correlation or by the column alias
     * <p> (simple identifier or prefixed with subquery alias)
     */
    DERIVED_COLUMN_NAME(false, true, 100),
    /**
     * Table name never referenced in the underlying contexts
     * <p> (simple identifier or fullname)
     */
    NEW_TABLE_NAME(true, false, 200),
    /**
     * Table name already used in the underlying contexts
     * <p> (simple identifier or fullname)
     */
    USED_TABLE_NAME(true, false, 200),
    /**
     * Table column name when derived from the real table 
     * <p> (simple identifier, fullname, alias-prefixed)
     * */
    TABLE_COLUMN_NAME(false, true, 100),
    /**
     * Composite field name
     * <p> (simple identifier)
     * */
    COMPOSITE_FIELD_NAME(false, true, 100),
    /**
     * Join condition based on the foreign key
     * <p> (expression, consisting of two column references)
     */
    JOIN_CONDITION(false, false, 0),
    /**
     * Procedure or function
     */
    PROCEDURE(false, false, 300),
    CATALOG(false, false, 275),
    SCHEMA(false, false, 250);


    public final boolean isTableName;
    public final boolean isColumnName;
    public final int sortOrder;

    SQLQueryCompletionItemKind(boolean isTableName, boolean isColumnName, int sortOrder) {
        this.isTableName = isTableName;
        this.isColumnName = isColumnName;
        this.sortOrder = sortOrder;
    }
}
    