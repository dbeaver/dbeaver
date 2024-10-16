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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

public abstract class SQLQueryCompletionItem {

    @NotNull
    private final SQLQueryWordEntry filterKey;
    
    private SQLQueryCompletionItem(@NotNull SQLQueryWordEntry filterKey) {
        this.filterKey = filterKey;
    }

    @NotNull
    public SQLQueryWordEntry getFilterInfo() {
        return this.filterKey;
    }

    @NotNull
    public abstract SQLQueryCompletionItemKind getKind();

    @Nullable
    public DBSObject getObject() {
        return null;
    }

    public final <R> R apply(SQLQueryCompletionItemVisitor<R> visitor) {
        return this.applyImpl(visitor);
    }

    protected abstract <R> R applyImpl(SQLQueryCompletionItemVisitor<R> visitor);

    /**
     * Prepare completion item for reserved word
     */
    @NotNull
    public static SQLQueryCompletionItem forReservedWord(@NotNull SQLQueryWordEntry filterKey, @NotNull String text) {
        return new SQLReservedWordCompletionItem(filterKey, text);
    }

    @NotNull
    public static SQLQueryCompletionItem forRowsSourceAlias(
        @NotNull SQLQueryWordEntry filterKey,
        @NotNull SQLQuerySymbol aliasSymbol,
        @NotNull SourceResolutionResult source
    ) {
        return new SQLRowsSourceAliasCompletionItem(filterKey, aliasSymbol, source);
    }

    @NotNull
    public static SQLQueryCompletionItem forRealTable(@NotNull SQLQueryWordEntry filterKey, @NotNull DBSEntity table, boolean isUsed) {
        return new SQLTableNameCompletionItem(filterKey, table, isUsed);
    }

    @NotNull
    public static SQLQueryCompletionItem forSubsetColumn(
        @NotNull SQLQueryWordEntry filterKey,
        @NotNull SQLQueryResultColumn columnInfo,
        @NotNull SourceResolutionResult sourceInfo,
        boolean absolute
    ) {
        return new SQLColumnNameCompletionItem(filterKey, columnInfo, sourceInfo, absolute);
    }

    @NotNull
    public static SQLQueryCompletionItem forDbObject(@NotNull SQLQueryWordEntry filterKey, @NotNull DBSObject object) {
        return new SQLDbNamedObjectCompletionItem(filterKey, object);
    }
    
    public static class SQLRowsSourceAliasCompletionItem extends SQLQueryCompletionItem {
        @NotNull
        public final SQLQuerySymbol symbol;
        @NotNull
        public final SourceResolutionResult sourceInfo;

        SQLRowsSourceAliasCompletionItem(
            @NotNull SQLQueryWordEntry filterKey,
            @NotNull SQLQuerySymbol symbol,
            @NotNull SourceResolutionResult sourceInfo
        ) {
            super(filterKey);
            this.symbol = symbol;
            this.sourceInfo = sourceInfo;
        }
        
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.SUBQUERY_ALIAS;
        }

        @Override
        protected <R> R applyImpl(SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitSubqueryAlias(this);
        }
    }
    
    public static class SQLColumnNameCompletionItem extends SQLQueryCompletionItem {
        @NotNull
        public final SQLQueryResultColumn columnInfo;
        @NotNull
        public final SourceResolutionResult sourceInfo;
        // TODO consider removing this flag in favor of refactoring for explicit formatting mechanism
        public final boolean absolute;

        SQLColumnNameCompletionItem(
            @NotNull SQLQueryWordEntry filterKey,
            @NotNull SQLQueryResultColumn columnInfo,
            @NotNull SourceResolutionResult sourceInfo,
            boolean absolute
        ) {
            super(filterKey);

            if (sourceInfo == null) {
                throw new IllegalArgumentException("sourceInfo should not be null");
            }
            if (columnInfo == null) {
                throw new IllegalArgumentException("columnInfo should not be null");
            }

            this.columnInfo = columnInfo;
            this.sourceInfo = sourceInfo;
            this.absolute = absolute;
        }
        
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return this.columnInfo.symbol.getSymbolClass() == SQLQuerySymbolClass.COLUMN_DERIVED 
                ? SQLQueryCompletionItemKind.DERIVED_COLUMN_NAME
                : SQLQueryCompletionItemKind.TABLE_COLUMN_NAME;
        }

        @Nullable
        @Override
        public DBSObject getObject() {
            return this.columnInfo.realAttr;
        }

        @Override
        protected <R> R applyImpl(SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitColumnName(this);
        }
    }
    
    public static class SQLTableNameCompletionItem extends SQLQueryCompletionItem {
        public final boolean isUsed;
        @NotNull
        public final DBSEntity table;

        SQLTableNameCompletionItem(@NotNull SQLQueryWordEntry filterKey, @NotNull DBSEntity table, boolean isUsed) {
            super(filterKey);
            this.isUsed = isUsed;
            this.table = table;
        }

        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return this.isUsed ? SQLQueryCompletionItemKind.USED_TABLE_NAME : SQLQueryCompletionItemKind.NEW_TABLE_NAME;
        }
        
        @Override
        public DBSObject getObject() {
            return this.table;
        }

        @Override
        protected <R> R applyImpl(SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitTableName(this);
        }
    }
    
    public static class SQLReservedWordCompletionItem extends SQLQueryCompletionItem {
        public final String text;

        SQLReservedWordCompletionItem(@NotNull SQLQueryWordEntry filterKey, @NotNull String text) {
            super(filterKey);
            this.text = text;
        }
    
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.RESERVED;
        }

        @NotNull
        @Override
        protected <R> R applyImpl(@NotNull SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitReservedWord(this);
        }
    }

    public static class SQLDbNamedObjectCompletionItem extends SQLQueryCompletionItem {

        @NotNull
        public final DBSObject object;

        SQLDbNamedObjectCompletionItem(@NotNull SQLQueryWordEntry filterKey, @NotNull DBSObject object) {
            super(filterKey);
            this.object = object;
        }

        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.UNKNOWN;
        }

        @NotNull
        @Override
        public DBSObject getObject() {
            return this.object;
        }

        @Override
        protected <R> R applyImpl(SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitNamedObject(this);
        }
    }
}
