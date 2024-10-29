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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Describes database entity name
 */
public class SQLQueryQualifiedName extends SQLQueryLexicalScopeItem {
    @NotNull
    public final List<SQLQuerySymbolEntry> scopeName;
    @NotNull
    public final SQLQuerySymbolEntry entityName;

    public final int invalidPartsCount;

    public SQLQueryQualifiedName(
        @NotNull STMTreeNode syntaxNode,
        @NotNull List<SQLQuerySymbolEntry> scopeName,
        @NotNull SQLQuerySymbolEntry entityName,
        int invalidPartsCount
    ) {
        super(syntaxNode);
        this.scopeName = scopeName;
        this.entityName = entityName;
        this.invalidPartsCount = invalidPartsCount;
    }

    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return entityName != null ? this.entityName.getSymbolClass() : SQLQuerySymbolClass.UNKNOWN;
    }

    @NotNull
    @Override
    public STMTreeNode[] getSyntaxComponents() {
        if (this.scopeName.isEmpty()) {
            return new STMTreeNode[] {
                this.entityName.getSyntaxNode()
            };
        } else {
            return Stream.of(
                this.scopeName.stream().map(SQLQueryLexicalScopeItem::getSyntaxNode),
                Stream.of(this.entityName.getSyntaxNode())
            ).flatMap(s -> s).toList().toArray(new STMTreeNode[0]);
        }
    }

    /**
     * Set the class to the qualified name components
     */
    public void setSymbolClass(@NotNull SQLQuerySymbolClass symbolClass) {
        this.entityName.getSymbol().setSymbolClass(symbolClass);
        this.scopeName.forEach(e -> e.getSymbol().setSymbolClass(symbolClass));
    }

    public void setDefinition(@NotNull DBSObject realObject) {
        SQLQuerySymbolClass entityNameClass  = realObject instanceof DBSTable || realObject instanceof DBSView
            ? SQLQuerySymbolClass.TABLE
            : SQLQuerySymbolClass.OBJECT;
        this.setDefinition(realObject, entityNameClass);
    }

    /**
     * Set the definition to the qualified name components based on the database metadata
     */
    public void setDefinition(@NotNull DBSObject realObject, SQLQuerySymbolClass entityNameClass) {
        this.entityName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(realObject, entityNameClass));
        DBSObject object = realObject.getParentObject();
        int scopeNameIndex = this.scopeName.size() - 1;
        while (object != null && scopeNameIndex >= 0) {
            SQLQuerySymbolEntry nameEntry = this.scopeName.get(scopeNameIndex);
            String objectName = SQLUtils.identifierToCanonicalForm(object.getDataSource().getSQLDialect(), DBUtils.getQuotedIdentifier(object), false, true);
            if (objectName.equalsIgnoreCase(nameEntry.getName())) {
                SQLQuerySymbolClass objectNameClass;
                if (object instanceof DBSSchema) {
                    objectNameClass = SQLQuerySymbolClass.SCHEMA;
                } else if (object instanceof DBSCatalog) {
                    objectNameClass = SQLQuerySymbolClass.CATALOG;
                } else {
                    objectNameClass = SQLQuerySymbolClass.UNKNOWN; // TODO consider OBJECT is not necessarily TABLE
                }
                nameEntry.setDefinition(new SQLQuerySymbolByDbObjectDefinition(object, objectNameClass));
                scopeNameIndex--;
            }
            object = object.getParentObject();
        }
    }

    /**
     * Set the definition to the qualified name components based on the query structure
     */
    public void setDefinition(@NotNull SourceResolutionResult rr) {
        if (rr.aliasOrNull != null) {
            this.entityName.setDefinition(rr.aliasOrNull.getDefinition());
        } else if (rr.source instanceof SQLQueryRowsTableDataModel tableModel) {
            SQLQueryQualifiedName tableName = tableModel.getName();
            if (tableName != null) {
                SQLQuerySymbolEntry lastDefSymbolEntry = tableName.entityName;
                this.entityName.setDefinition(lastDefSymbolEntry);
                int i = this.scopeName.size() - 1, j = tableName.scopeName.size() - 1;
                for (; i >= 0 && j >= 0; i--, j--) {
                    this.scopeName.get(i).setDefinition(lastDefSymbolEntry = tableName.scopeName.get(j));
                }
                while (i >= 0) {
                    this.scopeName.get(i).setDefinition(lastDefSymbolEntry);
                    i--;
                }
            }
        }
    }

    /**
     * Get list of the qualified name parts in the hierarchical order
     */
    @NotNull
    public List<String> toListOfStrings() {
        if (this.scopeName.isEmpty()) {
            return List.of(this.entityName.getName());
        } else {
            return Stream.of(
                this.scopeName.stream().map(SQLQuerySymbolEntry::getName),
                Stream.of(this.entityName.getName())
            ).flatMap(s -> s).toList();
        }
    }

    /**
     * Get the qualified name string representation
     */
    @NotNull
    public String toIdentifierString() {
        if (this.scopeName.isEmpty()) {
            return this.entityName.getRawName();
        } else {
            return String.join(".", this.toListOfStrings());
        }
    }

    @Override
    public String toString() {
        return String.join(".", this.toListOfStrings());
    }

    @Override
    public int hashCode() {
        return this.toListOfStrings().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SQLQueryQualifiedName other && this.toListOfStrings().equals(other.toListOfStrings());
    }

    public boolean isNotClassified() {
        return this.entityName.isNotClassified() && this.scopeName.stream().allMatch(SQLQuerySymbolEntry::isNotClassified);
    }

    public static void performPartialResolution(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics, SQLQueryQualifiedName name) {
        List<String> nameParts = new ArrayList<>(name.scopeName.size());
        boolean closed = false;
        for (SQLQuerySymbolEntry entry : name.scopeName) {
            if (entry != null) {
                nameParts.add(entry.getName());
            } else {
                closed = true;
                break;
            }
        }
        if (!closed && name.entityName != null) {
            nameParts.add(name.entityName.getName());
        }
        DBSObject object = context.findRealObject(statistics.getMonitor(), RelationalObjectType.TYPE_UNKNOWN, nameParts);
        if (object != null) {
            name.setDefinition(object);
        }
    }
}
