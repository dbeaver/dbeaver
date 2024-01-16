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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsTableDataModel;

import java.util.List;

public class SQLQueryQualifiedName { // qualifier
    
    public final SQLQuerySymbolEntry catalogName;
    public final SQLQuerySymbolEntry schemaName;
    public final SQLQuerySymbolEntry entityName;

    public SQLQueryQualifiedName(@NotNull SQLQuerySymbolEntry entityName) {
        this(null, null, entityName);
    }

    public SQLQueryQualifiedName(@Nullable SQLQuerySymbolEntry schemaName, @NotNull SQLQuerySymbolEntry entityName) {
        this(null, schemaName, entityName);
    }

    public SQLQueryQualifiedName(
        @Nullable SQLQuerySymbolEntry catalogName,
        @Nullable SQLQuerySymbolEntry schemaName,
        @NotNull SQLQuerySymbolEntry entityName
    ) {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.entityName = entityName;
    }
    
    public void setSymbolClass(@NotNull SQLQuerySymbolClass symbolClass) {
        if (this.entityName != null) {
            this.entityName.getSymbol().setSymbolClass(symbolClass);
        }
        if (this.schemaName != null) {
            this.schemaName.getSymbol().setSymbolClass(symbolClass);
        }
        if (this.catalogName != null) {
            this.catalogName.getSymbol().setSymbolClass(symbolClass);
        }
    }

    public void setDefinition(@NotNull DBSEntity realTable) {
        if (this.entityName != null) {
            this.entityName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(realTable, SQLQuerySymbolClass.TABLE));
            if (this.schemaName != null) {
                DBSObject schema = realTable.getParentObject();
                if (schema != null) {
                    this.schemaName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(schema, SQLQuerySymbolClass.SCHEMA));
                } else {
                    this.schemaName.getSymbol().setSymbolClass(SQLQuerySymbolClass.SCHEMA);
                }
                if (this.catalogName != null) {
                    DBSObject catalog = realTable.getParentObject();
                    if (catalog != null) {
                        this.catalogName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(catalog, SQLQuerySymbolClass.CATALOG));
                    } else {
                        this.catalogName.getSymbol().setSymbolClass(SQLQuerySymbolClass.CATALOG);
                    }
                }
            }
        }
    }

    public void setDefinition(@NotNull SourceResolutionResult rr) {
        if (rr.aliasOrNull != null) {
            this.entityName.merge(rr.aliasOrNull);
        } else if (rr.source instanceof SQLQueryRowsTableDataModel tableModel) {
            if (this.entityName != null) {
                this.entityName.setDefinition(tableModel);
                if (this.schemaName != null) {
                    this.entityName.setDefinition(tableModel);
                }
                if (this.catalogName != null) {
                    this.entityName.setDefinition(tableModel);
                }
            }
        }
    }

    @NotNull
    public List<String> toListOfStrings() {
        if (catalogName != null && schemaName != null) {
            return List.of(catalogName.getName(), schemaName.getName(), entityName.getName());
        } else if (schemaName != null) {
            return List.of(schemaName.getName(), entityName.getName());
        } else {
            return List.of(entityName.getName());
        }
    }

    @NotNull
    public String toIdentifierString() {
        if (catalogName != null && schemaName != null) {
            return String.join(".", catalogName.getRawName(), schemaName.getRawName(), entityName.getRawName());
        } else if (schemaName != null) {
            return String.join(".", schemaName.getRawName(), entityName.getRawName());
        } else {
            return entityName.getRawName();
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + String.join(".", this.toListOfStrings()) + "]";
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
        return this.entityName.isNotClassified();
    }
}
