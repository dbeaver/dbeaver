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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;

public abstract class SQLQueryCompletionItem {
    
    private SQLQueryCompletionItem() {
    }

    @NotNull
    public abstract SQLQueryCompletionItemKind getKind();

    @NotNull
    public abstract String getText();
    
    @Nullable
    public String getExtraText() {
        return null; 
    }

    @Nullable
    public String getDescription() {
        return null;
    }

    @Nullable
    public DBPNamedObject getObject() {
        return null;
    }

    /**
     * Prepare completion item for reserved word
     */
    @NotNull
    public static SQLQueryCompletionItem forReservedWord(@NotNull String text) {
        return new SQLReservedWordCompletionItem(text);
    }

    @NotNull
    public static SQLQueryCompletionItem forSubqueryAlias(@NotNull SQLQuerySymbol aliasSymbol) {
        return new SQLSubqueryAliasCompletionItem(aliasSymbol);
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
    public static SQLQueryCompletionItem forDbObject(@NotNull DBPNamedObject object) {
        return new SQLDbNamedObjectCompletionItem(object);
    }
    
    private static class SQLSubqueryAliasCompletionItem extends SQLQueryCompletionItem {
        @NotNull
        private final SQLQuerySymbol symbol;

        public SQLSubqueryAliasCompletionItem(@NotNull SQLQuerySymbol symbol) {
            this.symbol = symbol;
        }
        
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.SUBQUERY_ALIAS;
        }
        
        @NotNull
        @Override
        public String getText() {
            return this.symbol.getName();
        }
        
        @NotNull
        @Override
        public String getExtraText() {
            return "Subquery alias"; 
        }
        
        @NotNull
        @Override
        public String getDescription() {
            return "TODO"; // TODO add subquery text here
        }
    }
    
    private static class SQLColumnNameCompletionItem extends SQLQueryCompletionItem {
        @NotNull
        private final SQLQueryResultColumn columnInfo;
        @NotNull
        private final SourceResolutionResult sourceInfo;
        // TODO consider removing this flag in favor of refactoring for explicit formatting mechanism
        private final boolean absolute;

        public SQLColumnNameCompletionItem(
            @NotNull SQLQueryResultColumn columnInfo,
            @NotNull SourceResolutionResult sourceInfo,
            boolean absolute
        ) {
            this.columnInfo = columnInfo;
            this.sourceInfo = sourceInfo;
            this.absolute = absolute;
        }        
        
        @NotNull
        @Override
        public String getText() {
            if (this.absolute) {
                @NotNull String prefix = this.sourceInfo != null && this.sourceInfo.aliasOrNull != null ? this.sourceInfo.aliasOrNull.getName() + "." : "";
                return prefix + this.columnInfo.symbol.getName();
            } else {
                return this.columnInfo.symbol.getName();
            }
        }
        
        @Nullable
        @Override
        public String getExtraText() {
            @NotNull SQLQueryExprType type = this.columnInfo.type;
            @Nullable String typeName = type == null || type == SQLQueryExprType.UNKNOWN ? null : type.getDisplayName();
            return typeName == null ? null : (" : " + typeName);
        }
        
        @Override
        public String getDescription() {
            @Nullable String originalColumnName = this.columnInfo.realAttr == null ? null
                    : DBUtils.getObjectFullName(this.columnInfo.realAttr, DBPEvaluationContext.DML);

            if (this.columnInfo.symbol.getSymbolClass() == SQLQuerySymbolClass.COLUMN_DERIVED) {
                return "Derived column name " + (originalColumnName != null ? "for real column " + originalColumnName : "");
            } else {
                if (this.columnInfo.realAttr != null) {
                    return this.columnInfo.realAttr.getDescription();
                } else if (this.columnInfo.realSource != null) {
                    return "Column of the " +  DBUtils.getObjectFullName(this.columnInfo.realSource, DBPEvaluationContext.DML);
                } else {
                    return "Subquery column";
                }
            }
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
        public DBPNamedObject getObject() {
            return this.columnInfo.realAttr;
        }
    }
    
    private static class SQLTableNameCompletionItem extends SQLQueryCompletionItem {  
        private final boolean isUsed;
        @NotNull
        private final DBSEntity table;

        public SQLTableNameCompletionItem(@NotNull DBSEntity table, boolean isUsed) {
            this.isUsed = isUsed;
            this.table = table;
        }
        
        @NotNull
        @Override
        public String getText() {
            return this.table.getName();
        }

        @NotNull
        @Override
        public String getExtraText() {
            return (DBUtils.isView(this.table) ? "View " : "Table ") + DBUtils.getObjectFullName(this.table, DBPEvaluationContext.DML);
        }

        @Override
        public String getDescription() {
            return this.table.getDescription();
        }
        
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return this.isUsed ? SQLQueryCompletionItemKind.USED_TABLE_NAME : SQLQueryCompletionItemKind.NEW_TABLE_NAME;
        }
        
        @Override
        public DBPNamedObject getObject() {
            return this.table;
        }
    }
    
    private static class SQLReservedWordCompletionItem extends SQLQueryCompletionItem {
        private final String text;

        public SQLReservedWordCompletionItem(String text) {
            this.text = text;
        }
    
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.RESERVED;
        }
        
        @NotNull
        @Override
        public String getText() {
            return this.text;
        }
        
        @NotNull
        @Override
        public String getDescription() {
            return "Reserved word of the query language";
        }
    }

    private static class SQLDbNamedObjectCompletionItem extends SQLQueryCompletionItem {  
        private final DBPNamedObject object;

        public SQLDbNamedObjectCompletionItem(DBPNamedObject object) {
            this.object = object;
        }
        
        @NotNull
        @Override
        public String getText() {
            return this.object.getName();
        }
        
        @NotNull
        @Override
        public String getExtraText() {
            return DBUtils.getObjectFullName(this.object, DBPEvaluationContext.DML);
        }
        
        @NotNull
        @Override
        public String getDescription() {
            return this.getExtraText();
        }
        
        @NotNull
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.UNKNOWN;
        }
        
        @Override
        public DBPNamedObject getObject() {
            return this.object;
        }
    }
}
