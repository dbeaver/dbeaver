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
package org.jkiss.dbeaver.ui.editors.sql.semantics.completion;

import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;

public abstract class SQLQueryCompletionItem {
    
    private SQLQueryCompletionItem() {
    }
    
    public abstract SQLQueryCompletionItemKind getKind();
    
    public abstract String getText();
    
    public String getExtraText() { 
        return null; 
    } 
    
    public String getDescription() {
        return null;
    }
    
    public DBPNamedObject getObject() {
        return null;
    }
    
    
    public static SQLQueryCompletionItem forReservedWord(String text) {
        return new SQLReservedWordCompletionItem(text);
    }
    
    public static SQLQueryCompletionItem forSubqueryAlias(SQLQuerySymbol aliasSymbol) {
        return new SQLSubqueryAliasCompletionItem(aliasSymbol);
    }
    
    public static SQLQueryCompletionItem forRealTable(DBSEntity table, boolean isUsed) {
        return new SQLTableNameCompletionItem(table, isUsed);
    }
    
    public static SQLQueryCompletionItem forSubsetColumn(SQLQueryResultColumn columnInfo, SourceResolutionResult sourceInfo, boolean absolute) {
        return new SQLColumnNameCompletionItem(columnInfo, sourceInfo, absolute);
    }
    
    public static SQLQueryCompletionItem forDbObject(DBPNamedObject object) {
        return new SQLDbNamedObjectCompletionItem(object);
    }
    
    private static class SQLSubqueryAliasCompletionItem extends SQLQueryCompletionItem {
        private final SQLQuerySymbol symbol;

        public SQLSubqueryAliasCompletionItem(SQLQuerySymbol symbol) {
            this.symbol = symbol;
        }
        
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.SUBQUERY_ALIAS;
        }
        
        @Override
        public String getText() {
            return this.symbol.getName();
        }
        
        @Override
        public String getExtraText() {
            return "Subquery alias"; 
        }
        
        @Override
        public String getDescription() {
            return "TODO"; // TODO add subquery text here
        }
    }
    
    private static class SQLColumnNameCompletionItem extends SQLQueryCompletionItem {
        private final SQLQueryResultColumn columnInfo;
        private final SourceResolutionResult sourceInfo;
        private final boolean absolute;

        public SQLColumnNameCompletionItem(SQLQueryResultColumn columnInfo, SourceResolutionResult sourceInfo, boolean absolute) {
            this.columnInfo = columnInfo;
            this.sourceInfo = sourceInfo;
            this.absolute = absolute;
        }        
        
        @Override
        public String getText() {
            if (this.absolute) {
                String prefix = this.sourceInfo != null && this.sourceInfo.aliasOrNull != null ? this.sourceInfo.aliasOrNull.getName() + "." : ""; 
                return prefix + this.columnInfo.symbol.getName();
            } else {
                return this.columnInfo.symbol.getName();
            }
        }
        
        @Override
        public String getExtraText() {
            SQLQueryExprType type = this.columnInfo.type;
            String typeName = type == null || type == SQLQueryExprType.UNKNOWN ? null : type.getDisplayName();
            return typeName == null ? null : (" : " + typeName);
        }
        
        @Override
        public String getDescription() {
            String originalColumnName = this.columnInfo.realAttr == null ? null 
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
        
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return this.columnInfo.symbol.getSymbolClass() == SQLQuerySymbolClass.COLUMN_DERIVED 
                    ? SQLQueryCompletionItemKind.DERIVED_COLUMN_NAME
                    : SQLQueryCompletionItemKind.TABLE_COLUMN_NAME;
        }
        
        @Override
        public DBPNamedObject getObject() {
            return this.columnInfo.realAttr;
        }
    }
    
    private static class SQLTableNameCompletionItem extends SQLQueryCompletionItem {  
        private final boolean isUsed;
        private final DBSEntity table;

        public SQLTableNameCompletionItem(DBSEntity table, boolean isUsed) {
            this.isUsed = isUsed;
            this.table = table;
        }
        
        @Override
        public String getText() {
            return this.table.getName();
        }
        
        @Override
        public String getExtraText() {
            return (DBUtils.isView(this.table) ? "View " : "Table ") + DBUtils.getObjectFullName(this.table, DBPEvaluationContext.DML);
        }
        
        @Override
        public String getDescription() {
            return this.table.getDescription();
        }
        
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
    
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return SQLQueryCompletionItemKind.RESERVED;
        }
        
        @Override
        public String getText() {
            return this.text;
        }
        
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
        
        @Override
        public String getText() {
            return this.object.getName();
        }
        
        @Override
        public String getExtraText() {
            return DBUtils.getObjectFullName(this.object, DBPEvaluationContext.DML);
        }
        
        @Override
        public String getDescription() {
            return this.getExtraText();
        }
        
        @Override
        public SQLQueryCompletionItemKind getKind() {
            return null;
        }
        
        @Override
        public DBPNamedObject getObject() {
            return this.object;
        }
    }
}
