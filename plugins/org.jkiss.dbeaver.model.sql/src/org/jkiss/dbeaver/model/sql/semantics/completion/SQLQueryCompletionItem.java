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
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

public abstract class SQLQueryCompletionItem {
    
    private SQLQueryCompletionItem() {
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
    public static SQLQueryCompletionItem forReservedWord(@NotNull String text) {
        return new SQLReservedWordCompletionItem(text);
    }

    @NotNull
    public static SQLQueryCompletionItem forSubqueryAlias(@NotNull SQLQuerySymbol aliasSymbol, @NotNull SQLQueryRowsSourceModel source) {
        return new SQLSubqueryAliasCompletionItem(aliasSymbol, source);
    }

    @NotNull
    public static SQLQueryCompletionItem forRealTable(@NotNull DBSEntity table, boolean isUsed) {
        return new SQLTableNameCompletionItem(table, isUsed);
    }

    @NotNull
    public static SQLQueryCompletionItem forSubsetColumn(
        @NotNull SQLQueryResultColumn columnInfo,
        @NotNull SourceResolutionResult sourceInfo,
        boolean absolute
    ) {
        return new SQLColumnNameCompletionItem(columnInfo, sourceInfo, absolute);
    }

    @NotNull
    public static SQLQueryCompletionItem forDbObject(@NotNull DBSObject object) {
        return new SQLDbNamedObjectCompletionItem(object);
    }
    
    public static class SQLSubqueryAliasCompletionItem extends SQLQueryCompletionItem {
        @NotNull
        public final SQLQuerySymbol symbol;
        @NotNull
        public final SQLQueryRowsSourceModel source;

        SQLSubqueryAliasCompletionItem(@NotNull SQLQuerySymbol symbol, @NotNull SQLQueryRowsSourceModel source) {
            this.symbol = symbol;
            this.source = source;
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
            @NotNull SQLQueryResultColumn columnInfo,
            @NotNull SourceResolutionResult sourceInfo,
            boolean absolute
        ) {
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

        SQLTableNameCompletionItem(@NotNull DBSEntity table, boolean isUsed) {
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

        SQLReservedWordCompletionItem(@NotNull String text) {
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

        SQLDbNamedObjectCompletionItem(@NotNull DBSObject object) {
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
