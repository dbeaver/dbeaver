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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;

import java.util.LinkedList;
import java.util.List;

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
    public static SQLQueryCompletionItem forRealTable(
        @NotNull SQLQueryWordEntry filterKey,
        @Nullable ContextObjectInfo resolvedContext,
        @NotNull DBSEntity table, boolean isUsed
    ) {
        return new SQLTableNameCompletionItem(filterKey, resolvedContext, table, isUsed);
    }

    @NotNull
    public static SQLColumnNameCompletionItem forSubsetColumn(
        @NotNull SQLQueryWordEntry filterKey,
        @NotNull SQLQueryResultColumn columnInfo,
        @Nullable SourceResolutionResult sourceInfo,
        boolean absolute
    ) {
        return new SQLColumnNameCompletionItem(filterKey, columnInfo, sourceInfo, absolute);
    }

    @NotNull
    public static SQLQueryCompletionItem forDbObject(
        @NotNull SQLQueryWordEntry filterKey,
        @Nullable ContextObjectInfo resolvedContext,
        @NotNull DBSObject object
    ) {
        return new SQLDbNamedObjectCompletionItem(filterKey, resolvedContext, object);
    }

    public static SQLQueryCompletionItem forJoinCondition(
        @NotNull SQLQueryWordEntry filterKey,
        @NotNull SQLColumnNameCompletionItem first,
        @NotNull SQLColumnNameCompletionItem second) {
        return new SQLJoinConditionCompletionItem(filterKey, first, second);
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
        // should be null only for columns provided by the root projection (SELECT clause), because it doesn't serve as a source
        @Nullable
        public final SourceResolutionResult sourceInfo;
        // TODO consider removing this flag in favor of refactoring for explicit formatting mechanism
        public final boolean absolute;

        SQLColumnNameCompletionItem(
            @NotNull SQLQueryWordEntry filterKey,
            @NotNull SQLQueryResultColumn columnInfo,
            @Nullable SourceResolutionResult sourceInfo,
            boolean absolute
        ) {
            super(filterKey);

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

    public abstract static class SQLDbObjectCompletionItem<T extends DBSObject> extends SQLQueryCompletionItem {
        @Nullable
        public final ContextObjectInfo resolvedContext;
        @NotNull
        public final T object;

        SQLDbObjectCompletionItem(@NotNull SQLQueryWordEntry filterKey, @Nullable ContextObjectInfo resolvedContext, @NotNull T object) {
            super(filterKey);
            this.resolvedContext = resolvedContext;
            this.object = object;
        }

        @NotNull
        @Override
        public DBSObject getObject() {
            return this.object;
        }
    }

    public static class SQLTableNameCompletionItem extends SQLDbObjectCompletionItem<DBSEntity> {
        public final boolean isUsed;

        SQLTableNameCompletionItem(
            @NotNull SQLQueryWordEntry filterKey,
            @Nullable ContextObjectInfo resolvedContext,
            @NotNull DBSEntity table,
            boolean isUsed
        ) {
            super(filterKey, resolvedContext, table);
            this.isUsed = isUsed;
        }

        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return this.isUsed ? SQLQueryCompletionItemKind.USED_TABLE_NAME : SQLQueryCompletionItemKind.NEW_TABLE_NAME;
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

        @Override
        protected <R> R applyImpl(@NotNull SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitReservedWord(this);
        }
    }

    public static class SQLDbNamedObjectCompletionItem extends SQLDbObjectCompletionItem<DBSObject>  {

        SQLDbNamedObjectCompletionItem(
            @NotNull SQLQueryWordEntry filterKey,
            @Nullable ContextObjectInfo resolvedContext,
            @NotNull DBSObject object
        ) {
            super(filterKey, resolvedContext, object);
        }

        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.UNKNOWN;
        }

        @Override
        protected <R> R applyImpl(SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitNamedObject(this);
        }
    }

    public static class SQLJoinConditionCompletionItem extends SQLQueryCompletionItem {
        @NotNull
        public final SQLColumnNameCompletionItem left;
        @NotNull
        public final SQLColumnNameCompletionItem right;

        SQLJoinConditionCompletionItem(
            @NotNull SQLQueryWordEntry filterKey,
            @NotNull SQLColumnNameCompletionItem left,
            @NotNull SQLColumnNameCompletionItem right
        ) {
            super(filterKey);
            this.left = left;
            this.right = right;
        }

        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.JOIN_CONDITION;
        }

        @Override
        protected <R> R applyImpl(@NotNull SQLQueryCompletionItemVisitor<R> visitor) {
            return visitor.visitJoinCondition(this);
        }
    }

    public static List<String> prepareQualifiedNameParts(@NotNull DBSObject object, @Nullable DBSObject knownSubroot) {
        LinkedList<String> parts = new LinkedList<>();
        parts.addFirst(DBUtils.getQuotedIdentifier(object));
        for (DBSObject o = object.getParentObject(); o != knownSubroot; o = o.getParentObject()) {
            if (o instanceof DBSStructContainer) {
                parts.addFirst(DBUtils.getQuotedIdentifier(o));
            }
        }
        return parts;
    }

    public record ContextObjectInfo(@NotNull String string, @NotNull DBSObject object) {
    }
}
